/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.api.service.http

import java.io.{File, PrintWriter}
import javax.ws.rs.Path

import akka.pattern.ask
import com.stratio.sparta.serving.api.actor.EnvironmentActor._
import com.stratio.sparta.serving.api.constants.HttpConstant
import com.stratio.sparta.serving.core.exception.ServerException
import com.stratio.sparta.serving.core.helpers.SecurityManagerHelper.UnauthorizedResponse
import com.stratio.sparta.serving.core.models.ErrorModel
import com.stratio.sparta.serving.core.models.ErrorModel._
import com.stratio.sparta.serving.core.models.dto.LoggedUser
import com.stratio.sparta.serving.core.models.env.{Environment, EnvironmentData, EnvironmentVariable}
import com.wordnik.swagger.annotations._
import org.json4s.jackson.Serialization.write
import spray.http.HttpHeaders.`Content-Disposition`
import spray.http.StatusCodes
import spray.routing._

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Api(value = HttpConstant.EnvironmentPath, description = "Operations over environment", position = 0)
trait EnvironmentHttpService extends BaseHttpService {

  val genericError = ErrorModel(
    StatusCodes.InternalServerError.intValue,
    EnvironmentServiceUnexpected,
    ErrorCodesMessages.getOrElse(EnvironmentServiceUnexpected, UnknownError)
  )

  override def routes(user: Option[LoggedUser] = None): Route =
    find(user) ~ update(user) ~ create(user) ~ deleteEnv(user) ~ exportData(user) ~ importData(user) ~
      createVariable(user) ~ updateVariable(user) ~ deleteVariable(user) ~ findVariable(user)

  @ApiOperation(value = "Find environment",
    notes = "Returns an environment with all variables",
    httpMethod = "GET",
    response = classOf[Environment]
  )
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def find(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath) {
      get {
        context =>
          for {
            response <- (supervisor ? FindEnvironment(user)).mapTo[Either[ResponseEnvironment, UnauthorizedResponse]]
          } yield getResponse(context, EnvironmentServiceFindEnvironment, response, genericError)
      }
    }
  }

  @Path("/variable/{name}")
  @ApiOperation(value = "Finds an variable by its name",
    notes = "Find a variable by its name",
    httpMethod = "GET",
    response = classOf[EnvironmentVariable])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "name",
      value = "name of the environment variable",
      dataType = "string",
      required = true,
      paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)
  ))
  def findVariable(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath / "variable" / Segment) { (name) =>
      get {
        context =>
          for {
            response <- (supervisor ? FindEnvironmentVariable(name, user))
              .mapTo[Either[ResponseEnvironmentVariable, UnauthorizedResponse]]
          } yield getResponse(context, EnvironmentServiceFindEnvironmentVariable, response, genericError)
      }
    }
  }

  @Path("")
  @ApiOperation(value = "Delete environment",
    notes = "Deletes the environment variables",
    httpMethod = "DELETE")
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def deleteEnv(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath) {
      pathEndOrSingleSlash {
        delete {
          complete {
            for {
              response <- (supervisor ? DeleteEnvironment(user)).mapTo[Either[Response, UnauthorizedResponse]]
            } yield deletePostPutResponse(EnvironmentServiceDeleteEnvironment, response, genericError, StatusCodes.OK)
          }
        }
      }
    }
  }

  @Path("/variable/{name}")
  @ApiOperation(value = "Deletes an environment variable by its name",
    notes = "Deletes an environment variable by its name",
    httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "name",
      value = "name of the environment variable",
      dataType = "string",
      required = true,
      paramType = "path")
  ))
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def deleteVariable(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath / "variable" / Segment) { (name) =>
      delete {
        complete {
          for {
            response <- (supervisor ? DeleteEnvironmentVariable(name, user))
              .mapTo[Either[ResponseEnvironment, UnauthorizedResponse]]
          } yield {
            deletePostPutResponse(EnvironmentServiceDeleteEnvironmentVariable, response, genericError, StatusCodes.OK)
          }
        }
      }
    }
  }

  @ApiOperation(value = "Updates an environment.",
    notes = "Updates an environment.",
    httpMethod = "PUT",
    response = classOf[Environment]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "environment",
      value = "environment json",
      dataType = "Environment",
      required = true,
      paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)
  ))
  def update(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath) {
      put {
        entity(as[Environment]) { request =>
          complete {
            for {
              response <- (supervisor ? UpdateEnvironment(request, user))
                .mapTo[Either[ResponseEnvironmentVariable, UnauthorizedResponse]]
            } yield deletePostPutResponse(EnvironmentServiceUpdateEnvironment, response, genericError, StatusCodes.OK)
          }
        }
      }
    }
  }

  @Path("/variable")
  @ApiOperation(value = "Updates an environment variable.",
    notes = "Updates an environment variable.",
    httpMethod = "PUT",
    response = classOf[Environment]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "environment variable",
      value = "environment variable json",
      dataType = "EnvironmentVariable",
      required = true,
      paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)
  ))
  def updateVariable(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath / "variable") {
      put {
        entity(as[EnvironmentVariable]) { request =>
          complete {
            for {
              response <- (supervisor ? UpdateEnvironmentVariable(request, user))
                .mapTo[Either[ResponseEnvironment, UnauthorizedResponse]]
            } yield deletePostPutResponse(EnvironmentServiceUpdateEnvironment, response, genericError, StatusCodes.OK)
          }
        }
      }
    }
  }

  @ApiOperation(value = "Creates a environment",
    notes = "Returns the environment",
    httpMethod = "POST",
    response = classOf[Environment])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "environment",
      value = "environment json",
      dataType = "Environment",
      required = true,
      paramType = "body")))
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def create(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath) {
      post {
        entity(as[Environment]) { request =>
          complete {
            for {
              response <- (supervisor ? CreateEnvironment(request, user))
                .mapTo[Either[ResponseEnvironment, UnauthorizedResponse]]
            } yield deletePostPutResponse(EnvironmentServiceCreateEnvironment, response, genericError)
          }
        }
      }
    }
  }

  @Path("/variable")
  @ApiOperation(value = "Creates a environment variable",
    notes = "Returns the environment variable",
    httpMethod = "POST",
    response = classOf[EnvironmentVariable])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "environment variable",
      value = "environment variable json",
      dataType = "EnvironmentVariable",
      required = true,
      paramType = "body")))
  @ApiResponses(
    Array(new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)))
  def createVariable(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath / "variable") {
      post {
        entity(as[EnvironmentVariable]) { request =>
          complete {
            for {
              response <- (supervisor ? CreateEnvironmentVariable(request, user))
                .mapTo[Either[ResponseEnvironmentVariable, UnauthorizedResponse]]
            } yield deletePostPutResponse(EnvironmentServiceCreateEnvironmentVariable, response, genericError)
          }
        }
      }
    }
  }

  @Path("/export")
  @ApiOperation(value = "Export data to other environment",
    notes = "Export data to environment migration",
    httpMethod = "GET",
    response = classOf[EnvironmentData])
  @ApiResponses(Array(
    new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)
  ))
  def exportData(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath / "export") {
      get {
        onComplete(environmentDataTempFile(user)) {
          case Success((envData, tempFile)) =>
            respondWithHeader(`Content-Disposition`("attachment", Map("filename" -> s"environmentData.json"))) {
              val printWriter = new PrintWriter(tempFile)
              try {
                printWriter.write(write(envData))
              } finally {
                printWriter.close()
              }
              getFromFile(tempFile)
            }
          case Failure(ex) => throw ex
        }
      }
    }
  }

  @Path("/import")
  @ApiOperation(value = "Import environment data.",
    notes = "Import environment data",
    httpMethod = "PUT",
    response = classOf[EnvironmentData]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "environment data",
      value = "environment data json",
      dataType = "EnvironmentData",
      required = true,
      paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = HttpConstant.NotFound,
      message = HttpConstant.NotFoundMessage)
  ))
  def importData(user: Option[LoggedUser]): Route = {
    path(HttpConstant.EnvironmentPath / "import") {
      put {
        entity(as[EnvironmentData]) { request =>
          complete {
            for {
              response <- (supervisor ? ImportData(request, user))
                .mapTo[Either[ResponseEnvironmentData, UnauthorizedResponse]]
            } yield deletePostPutResponse(EnvironmentServiceImportData, response, genericError, StatusCodes.OK)
          }
        }
      }
    }
  }

  private def environmentDataTempFile(user: Option[LoggedUser]): Future[(EnvironmentData, File)] = {
    for {
      response <- (supervisor ? ExportData(user))
        .mapTo[Either[ResponseEnvironmentData, UnauthorizedResponse]]
    } yield response match {
      case Left(Failure(e)) =>
        throw new ServerException(ErrorModel.toString(ErrorModel(
          StatusCodes.InternalServerError.intValue,
          EnvironmentServiceExportData,
          ErrorCodesMessages.getOrElse(EnvironmentServiceExportData, UnknownError),
          None,
          Option(e.getLocalizedMessage)
        )))
      case Left(Success(environmentData: EnvironmentData)) =>
        val tempFile = File.createTempFile(s"environmentData-${System.currentTimeMillis()}", ".json")
        tempFile.deleteOnExit()
        (environmentData, tempFile)
      case Right(UnauthorizedResponse(exception)) =>
        throw exception
      case _ =>
        throw new ServerException(ErrorModel.toString(genericError))
    }
  }
}
