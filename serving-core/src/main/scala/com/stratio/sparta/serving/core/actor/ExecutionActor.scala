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

package com.stratio.sparta.serving.core.actor

import akka.actor.{Actor, ActorRef}
import com.stratio.sparta.security._
import com.stratio.sparta.serving.core.actor.ExecutionActor._
import com.stratio.sparta.serving.core.actor.ExecutionInMemoryApi._
import com.stratio.sparta.serving.core.models.dto.LoggedUser
import com.stratio.sparta.serving.core.models.workflow.WorkflowExecution
import com.stratio.sparta.serving.core.services.ExecutionService
import com.stratio.sparta.serving.core.utils.ActionUserAuthorize
import org.apache.curator.framework.CuratorFramework

class ExecutionActor(val curatorFramework: CuratorFramework, inMemoryApiExecution: ActorRef)
                    (implicit val secManagerOpt: Option[SpartaSecurityManager])
  extends Actor with ActionUserAuthorize {

  private val executionService = new ExecutionService(curatorFramework)
  private val ResourceType = "execution"

  override def receive: Receive = {
    case CreateExecution(request, user) => createExecution(request, user)
    case Update(request, user) => updateExecution(request, user)
    case FindAll(user) => findAllExecutions(user)
    case FindById(id, user) => findExecutionById(id, user)
    case DeleteAll(user) => deleteAllExecutions(user)
    case DeleteExecution(id, user) => deleteExecution(id, user)
    case _ => log.info("Unrecognized message in Workflow Execution Actor")
  }

  def createExecution(request: WorkflowExecution, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer(user, Map(ResourceType -> Create)) {
      executionService.create(request)
    }

  def updateExecution(request: WorkflowExecution, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer(user, Map(ResourceType -> Edit)) {
      executionService.update(request)
    }

  def findAllExecutions(user: Option[LoggedUser]): Unit =
    securityActionAuthorizer(
      user,
      Map(ResourceType -> View),
      Option(inMemoryApiExecution)
    ) {
      FindAllMemoryExecution
    }

  def findExecutionById(id: String, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer(
      user,
      Map(ResourceType -> View),
      Option(inMemoryApiExecution)
    ) {
      FindMemoryExecution(id)
    }

  def deleteAllExecutions(user: Option[LoggedUser]): Unit =
    securityActionAuthorizer(user, Map(ResourceType -> Delete)) {
      executionService.deleteAll
    }


  def deleteExecution(id: String, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer(user, Map(ResourceType -> Delete)) {
      executionService.delete(id)
    }

}

object ExecutionActor {

  case class Update(request: WorkflowExecution, user: Option[LoggedUser])

  case class CreateExecution(request: WorkflowExecution, user: Option[LoggedUser])

  case class DeleteExecution(id: String, user: Option[LoggedUser])

  case class DeleteAll(user: Option[LoggedUser])

  case class FindAll(user: Option[LoggedUser])

  case class FindById(id: String, user: Option[LoggedUser])

}
