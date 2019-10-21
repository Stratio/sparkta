/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.gosec.dyplon.plugins.sparta

import java.io.InputStream
import java.net.InetAddress

import com.stratio.gosec.facade.DyplonFacadeAuthorizer
import com.stratio.sparta.security._

import scala.io.Source
import scala.util.Try
import com.stratio.sparta.security.{Action => SpartaAction, Create => SpartaCreate, Delete => SpartaDelete, Describe => SpartaDescribe, Download => SpartaDownload, Edit => SpartaEdit, Select => SpartaSelect, Status => SpartaStatus, Upload => SpartaUpload, View => SpartaView, Manage => SpartaManage}
import org.json4s.ext.DateTimeSerializer
import org.json4s.jackson.Serialization._
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory

case class ScopeHttp(`type`: String, actions: Seq[String])

case class PluginScopeHttp(scope: Seq[ScopeHttp])

class GoSecSpartaSecurityManagerFacade extends SpartaSecurityManager {

  implicit class NotBlankOption(s: Option[String]) {

    def notBlank: Option[String] = s.map(_.trim).filterNot(_.isEmpty)
  }

  implicit def json4sJacksonFormats: Formats = {
    DefaultFormats + DateTimeSerializer
  }

  implicit def actionConversion(spartaAction: SpartaAction): String = spartaAction match {
    case SpartaView => "view"
    case SpartaCreate => "create"
    case SpartaDelete => "delete"
    case SpartaEdit => "edit"
    case SpartaStatus => "status"
    case SpartaDownload => "download"
    case SpartaUpload => "upload"
    case SpartaDescribe => "describe"
    case SpartaSelect => "select"
    case SpartaManage => "manage"
  }

  lazy val logger = LoggerFactory.getLogger(classOf[GoSecSpartaSecurityManagerFacade])

  override def start: Unit = {

    logger.info(s"Starting Sparta $spartaInstance plugin")
    logger.info(s"Plugin registration parameters: serviceName=$serviceName, version=$spartaVersion, authorizer=$spartaAuthorizer")

    val spartaPlugin = parseSpartaPlugin.scope.flatMap(s => Map(s.`type` -> s.actions)).toMap
    val registerPluginResponse = DyplonFacadeAuthorizer.registerPlugin(
      serviceName,
      spartaVersion,
      spartaInstance,
      spartaPlugin,
      serviceUrl = None,
      spartaAuthorizer,
      posix = true,
      authorizationMode = Some("resource")
    )

    if (registerPluginResponse)
      logger.info(s"Sparta plugin instance [$spartaInstance], version [$spartaVersion]," +
        s" service $serviceName with authorizer [$spartaAuthorizer] is already registered")
    else
      throw new RuntimeException("Sparta plugin registration failed")

  }

  override def authorize(
                          userId: String,
                          resource: Resource,
                          action: Action,
                          hierarchy: Boolean
                        ): Boolean =

    DyplonFacadeAuthorizer.authorize(
      userId,
      action,
      serviceName,
      spartaVersion,
      spartaInstance,
      resource.resourceType.name(),
      resource.name,
      getLocalIp,
      false
    )

  /** PRIVATE METHODS */

  private def parseSpartaPlugin: PluginScopeHttp = {
    val stream: InputStream = getClass.getResourceAsStream("/manifest-sparta-http.json")
    val json: String = Source.fromInputStream(stream).mkString
    read[PluginScopeHttp](json)
  }

  private def getLocalIp: String = {
    if (Try(dyplonApiConfig.getString("local.hostname")).isSuccess)
      dyplonApiConfig.getString("local.hostname")
    else
      InetAddress.getLocalHost.getHostAddress
  }

  override def audit(auditEvent: AuditEvent): Unit = ()

  override def stop: Unit = {
    logger.info(s"Stopping client for instance: $spartaInstance")
  }
}