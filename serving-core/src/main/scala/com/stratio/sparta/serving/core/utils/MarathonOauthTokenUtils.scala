/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.core.utils

import java.net.HttpCookie

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.core.helpers.ExceptionHelper
import com.stratio.sparta.serving.core.config.SpartaConfig
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpOptions, HttpResponse}

object MarathonOauthTokenUtils extends SLF4JLogging {

  lazy private val marathonConfig: Config = SpartaConfig.getMarathonConfig().get
  lazy private val dcosAuthCookieName = "dcos-acs-auth-cookie"
  lazy private val ssoUriField = "sso.uri"
  lazy private val usernameField = "sso.username"
  lazy private val passwordField = "sso.password"
  lazy private val clientIdField = "sso.clientId"
  lazy private val redirectUriField = "sso.redirectUri"
  lazy private val retriesField = "sso.retries"
  lazy private val defaultRetries = 10

  @volatile
  private var currentToken: Option[HttpCookie] = None

  //scalastyle:off
  def getToken: HttpCookie = {
    synchronized {
      currentToken match {
        case Some(t) if !t.hasExpired =>
          log.debug(s"Cookie not expired with maxAge: ${t.getMaxAge}")
          t
        case Some(t) if t.hasExpired =>
          log.debug(s"Cookie expired with maxAge ${t.getMaxAge}")
          tokenWithExpiration
        case _ =>
          log.debug(s"Getting new cookie")
          tokenWithExpiration
      }
    }
  }

  def expireToken(): Unit = currentToken.foreach(cookie => cookie.setMaxAge(0))

  private def tokenWithExpiration: HttpCookie = {
    Try(retryGetToken(1)(retrieveTokenFromSSO)) match {
      case Success(token) =>
        log.debug(s"Cookie obtained successful")
        token.setMaxAge(5 * 60 * 60) //5 hours
        log.debug(s"Modified cookie MaxAge: ${token.getMaxAge}")
        currentToken = Option(token)
        token
      case Failure(ex) =>
        log.error(s"Error getting token for Marathon API requests, ${ex.getLocalizedMessage}", ex)
        currentToken = None
        throw new Exception(s"ERROR: ${ex.getMessage}." +
          s" For further information, please consult the application log")
    }
  }

  private def retryGetToken(numberCurrentRetries: Int)(getTokenFn: => Try[Seq[HttpCookie]]): HttpCookie = {
    val maxRetries = Try(marathonConfig.getInt(retriesField))
      .getOrElse(defaultRetries)

    if (numberCurrentRetries <= maxRetries) {
      retrieveTokenFromSSO match {
        case Success(tokenCookie) =>
          val dcosCookie = tokenCookie.filter(cookie =>
            cookie.getName.equalsIgnoreCase(dcosAuthCookieName))
          if (dcosCookie.nonEmpty) {
            log.debug(
              s"""Marathon Token "$dcosAuthCookieName" correctly retrieved """ +
                s"at retry attempt n. $numberCurrentRetries")
            dcosCookie.head
          }
          else {
            log.debug(s"Retry attempt n. $numberCurrentRetries :" +
              s"Error trying to recover the Oauth token: cookie $dcosAuthCookieName not found")
            retryGetToken(numberCurrentRetries + 1)(getTokenFn)
          }
        case Failure(ex) =>
          log.warn(s"Retry attempt n. $numberCurrentRetries :" +
            s" Error trying to recover Oauth token: ${ExceptionHelper.toPrintableException(ex)}", ex)
          retryGetToken(numberCurrentRetries + 1)(getTokenFn)
      }
    } else {
      throw new Exception(s"It was not possible to recover the Oauth token " +
        s"$dcosAuthCookieName after $maxRetries retries")
    }
  }

  private def retrieveTokenFromSSO: Try[Seq[HttpCookie]] =
    Try {
      //Configuration properties
      val ssoUri = Try(marathonConfig.getString(ssoUriField))
        .getOrElse(throw new Exception("SSO Uri not defined"))
      val clientId = Try(marathonConfig.getString(clientIdField))
        .getOrElse(throw new Exception("clientId not defined"))
      val redirectUri = Try(marathonConfig.getString(redirectUriField))
        .getOrElse(throw new Exception("redirectUri not defined"))

      // First request (AUTHORIZE)
      val authRequest = s"$ssoUri/oauth2.0/authorize?redirect_uri=$redirectUri&client_id=$clientId"
      log.debug(s"1. Request authorize to : $authRequest")
      val authResponse = Http(authRequest)
        .option(HttpOptions.followRedirects(false))
        .asString
      val jSessionIdAndSSOIdCookies = getCookies(authResponse)
      log.debug(s"1. Response: $authResponse")

      // Second request (Redirect to LOGIN)
      val redirectToLogin = extractRedirectUriFromLocationHeader(authResponse)
      log.debug(s"2. Request login redirect to : $redirectToLogin with cookies ${jSessionIdAndSSOIdCookies.keys.mkString(",")}")
      val postFormUri = Http(redirectToLogin)
        .cookies(jSessionIdAndSSOIdCookies.values.toSeq)
        .asString
      val (lt, execution) = extractLTAndExecution(postFormUri.body)
      log.debug(s"2. Response: $postFormUri")

      // Third request (login POST)
      val loginPostUri = s"$ssoUri/login?service=$ssoUri/oauth2.0/callbackAuthorize"
      val ssoRedirectionCookie = Map("sso_redirection" -> new HttpCookie("sso_redirection", s"${ssoUri.substring(0, ssoUri.lastIndexOf(":"))}/"))
      val loginPostCookies = jSessionIdAndSSOIdCookies ++ ssoRedirectionCookie
      val loginForm = createLoginForm(lt, execution)
      log.debug(s"3. Request post login to $loginPostUri with cookies ${loginPostCookies.keys.mkString(",")}")
      val loginRequest = Http(loginPostUri)
        .cookies(loginPostCookies.values.toSeq)
        .option(HttpOptions.followRedirects(false))
        .postForm(loginForm)
        .asString
      val casPrivacyAndTgcCookies = getCookies(loginRequest)
      log.debug(s"3. Response: $loginRequest")

      // Fourth request (Redirect from POST)
      val callbackUri = extractRedirectUriFromLocationHeader(loginRequest)
      val redirectPostCookies = casPrivacyAndTgcCookies ++ loginPostCookies
      log.debug(s"4. Request redirect post to : $callbackUri with cookies ${redirectPostCookies.keys.mkString(",")}")
      val ticketResponse = Http(callbackUri)
        .cookies(redirectPostCookies.values.toSeq)
        .option(HttpOptions.followRedirects(false))
        .asString
      log.debug(s"4. Response: $ticketResponse")

      // Fifth request (Redirect with Ticket)
      val clientRedirectUri = extractRedirectUriFromLocationHeader(ticketResponse)
      val clientRedirectCookies = loginPostCookies
      log.debug(s"5. Request redirect with ticket to : $clientRedirectUri with cookies ${clientRedirectCookies.keys.mkString(",")}")
      val tokenResponse = Http(clientRedirectUri)
        .cookies(clientRedirectCookies.values.toSeq)
        .asString
      log.debug(s"5. Response: $tokenResponse")

      val cookiesToReturn = getCookies(tokenResponse).values.toSeq
      log.debug(s"Cookies to return ${cookiesToReturn.map(_.getName).mkString(",")}")
      cookiesToReturn
    }

  private def extractRedirectUriFromLocationHeader(response: HttpResponse[String]): String = {
    Try(response.headers("Location").head).getOrElse {
      throw new Exception(s"The response not contains the location header, the available headers are: ${response.headers.keys.mkString(",")}")
    }
  }

  private def extractLTAndExecution(body: String): (String, String) = {
    val ltLEftMAtch = "name=\"lt\" value=\""
    val lt1 = body.indexOf(ltLEftMAtch)
    val prelt = body.substring(lt1 + ltLEftMAtch.length)
    val lt = prelt.substring(0, prelt.indexOf("\" />")).trim

    val executionLEftMAtch = "name=\"execution\" value=\""
    val execution1 = body.indexOf(executionLEftMAtch)
    val execution = body.substring(execution1 + executionLEftMAtch.length).split("\"")(0)

    (lt, execution)
  }

  private def createLoginForm(lt: String, execution: String): Seq[(String, String)] = {
    val userName = Try(marathonConfig.getString(usernameField))
      .getOrElse(throw new Exception("username not defined"))
    val password = Try(marathonConfig.getString(passwordField))
      .getOrElse(throw new Exception("password not defined"))

    Seq(
      "lt" -> lt,
      "_eventId" -> "submit",
      "execution" -> execution,
      "submit" -> "LOGIN",
      "username" -> userName,
      "password" -> password,
      "tenant" -> "NONE"
    )
  }

  private def getCookies(response: HttpResponse[String]): Map[String, HttpCookie] = {
    val headersCookies = response.headers.get("Set-Cookie") match {
      case Some(cookies) =>
        cookies.flatMap { cookie =>
          val cookieFields = cookie.split(";")(0).split("=").map(_.trim)
          if(cookieFields.length == 2 && cookieFields(0).nonEmpty && cookieFields(1).nonEmpty)
            Option(cookieFields(0) -> new HttpCookie(cookieFields(0), cookieFields(1)))
          else None
        }.toMap
      case None => Map.empty[String, HttpCookie]
    }

    response.cookies.flatMap { cookie =>
      if (cookie.getName.nonEmpty)
        Option(cookie.getName -> cookie)
      else None
    }.toMap ++ headersCookies
  }
}
