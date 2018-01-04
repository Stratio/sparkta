/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.sparta.serving.api.actor

import akka.actor.{Actor, _}
import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.security.{Describe, Execute, SpartaSecurityManager, View}
import com.stratio.sparta.serving.api.actor.CrossdataActor._
import com.stratio.sparta.serving.api.services.CrossdataService
import com.stratio.sparta.serving.core.models.crossdata.{QueryRequest, TableInfoRequest, TablesRequest}
import com.stratio.sparta.serving.core.models.dto.LoggedUser
import com.stratio.sparta.serving.core.services.HdfsService
import com.stratio.sparta.serving.core.utils.ActionUserAuthorize

import scala.util.Try

class CrossdataActor(implicit val secManagerOpt: Option[SpartaSecurityManager]) extends Actor
  with SLF4JLogging
  with ActionUserAuthorize {

  lazy val crossdataService = new CrossdataService
  val ResourceType = "catalog"

  lazy val hdfsWithUgiService = Try(HdfsService()).toOption
    .flatMap(utils => utils.ugiOption.flatMap( _ => Option(utils)))

  override def receive: Receive = {
    case FindAllDatabases(user) => findAllDatabases(user)
    case FindAllTables(user) => findAllTables(user)
    case FindTables(tablesRequest, user) => findTables(tablesRequest, user)
    case DescribeTable(tableInfoRequest, user) => describeTable(tableInfoRequest, user)
    case ExecuteQuery(queryRequest, user) => executeQuery(queryRequest, user)
    case _ => log.info("Unrecognized message in CrossdataActor")
  }

  /**
    * Runs `f` using `HdfsUtils#runFunction` if possible.
    * otherwise, just invoke `f`
    */
  def maybeWithHdfsUgiService(f: => Unit): Unit = hdfsWithUgiService.map(_.runFunction(f)).getOrElse(f)

  def findAllDatabases(user: Option[LoggedUser]): Unit = maybeWithHdfsUgiService {
    securityActionAuthorizer(user, Map(ResourceType -> View)) {
      crossdataService.listDatabases
    }
  }

  def findAllTables(user: Option[LoggedUser]): Unit = maybeWithHdfsUgiService {
    securityActionAuthorizer(user, Map(ResourceType -> View)) {
      crossdataService.listAllTables
    }
  }

  def findTables(tablesRequest: TablesRequest, user: Option[LoggedUser]): Unit = maybeWithHdfsUgiService {
    securityActionAuthorizer(user, Map(ResourceType -> View)) {
      crossdataService.listTables(tablesRequest.dbName.notBlank, tablesRequest.temporary)
    }
  }


  def describeTable(tableInfoRequest: TableInfoRequest, user: Option[LoggedUser]): Unit = maybeWithHdfsUgiService {
    securityActionAuthorizer(user, Map(ResourceType -> View)) {
      crossdataService.listColumns(tableInfoRequest.tableName, tableInfoRequest.dbName)
    }
  }

  def executeQuery(queryRequest: QueryRequest, user: Option[LoggedUser]): Unit = maybeWithHdfsUgiService {
    securityActionAuthorizer(user, Map(ResourceType -> View)) {
      crossdataService.executeQuery(queryRequest.query)
    }
  }

}

object CrossdataActor {

  case class FindAllDatabases(user: Option[LoggedUser])

  case class FindAllTables(user: Option[LoggedUser])

  case class FindTables(tablesRequest: TablesRequest, user: Option[LoggedUser])

  case class DescribeTable(tableInfoRequest: TableInfoRequest, user: Option[LoggedUser])

  case class ExecuteQuery(queryRequest: QueryRequest, user: Option[LoggedUser])

}
