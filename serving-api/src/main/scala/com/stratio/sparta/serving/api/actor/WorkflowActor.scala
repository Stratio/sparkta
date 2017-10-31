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

import akka.actor.{Actor, ActorRef}
import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.security._
import com.stratio.sparta.serving.core.exception.ServerException
import com.stratio.sparta.serving.core.models.dto.LoggedUser
import com.stratio.sparta.serving.core.models.workflow.Workflow
import com.stratio.sparta.serving.core.services.WorkflowService
import com.stratio.sparta.serving.core.utils.{ActionUserAuthorize, CheckpointUtils}
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.KeeperException.NoNodeException

import scala.util.Try

class WorkflowActor(val curatorFramework: CuratorFramework, statusActor: => ActorRef)(
  implicit val secManagerOpt: Option[SpartaSecurityManager]
) extends Actor with CheckpointUtils with ActionUserAuthorize {

  import WorkflowActor._

  //TODO change dyplon to new names: policy -> workflow
  val ResourcePol = "policy"
  val ResourceCP = "checkpoint"
  val ResourceContext = "context"
  private val workflowService = new WorkflowService(curatorFramework)

  //scalastyle:off
  override def receive: Receive = {
    case CreateWorkflow(workflow, user) => create(workflow, user)
    case CreateWorkflows(workflows, user) => createList(workflows, user)
    case Update(workflow, user) => update(workflow, user)
    case UpdateList(workflows, user) => updateList(workflows, user)
    case Find(id, user) => find(id, user)
    case FindByIdList(workflowIds, user) => findByIdList(workflowIds, user)
    case FindByName(name, user) => findByName(name.toLowerCase, user)
    case FindAll(user) => findAll(user)
    case DeleteWorkflow(id, user) => delete(id, user)
    case DeleteList(workflowIds, user) => deleteList(workflowIds, user)
    case DeleteAll(user) => deleteAll(user)
    case FindByTemplateType(fragmentType, user) => findByTemplateType(fragmentType, user)
    case FindByTemplateName(fragmentType, name, user) => findByTemplateName(fragmentType, name, user)
    case DeleteCheckpoint(name, user) => deleteCheckpoint(name, user)
    case _ => log.info("Unrecognized message in Workflow Actor")
  }

  //scalastyle:on

  def findAll(user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[ResponseWorkflows](user, Map(ResourcePol -> View)) {
      Try {
        workflowService.findAll
      } recover {
        case _: NoNodeException => Seq.empty[Workflow]
      }
    }

  def findByTemplateType(fragmentType: String, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[ResponseWorkflows](user, Map(ResourcePol -> View)) {
      Try(workflowService.findByTemplateType(fragmentType)).recover {
        case _: NoNodeException => Seq.empty[Workflow]
      }
    }

  def findByTemplateName(fragmentType: String, name: String, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[ResponseWorkflows](user, Map(ResourcePol -> View)) {
      Try(workflowService.findByTemplateName(fragmentType, name)).recover {
        case _: NoNodeException => Seq.empty[Workflow]
      }
    }

  def find(id: String, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[ResponseWorkflow](user, Map(ResourcePol -> View)) {
      Try(workflowService.findById(id)).recover {
        case _: NoNodeException =>
          throw new ServerException(s"No workflow with id $id.")
      }
    }

  def findByIdList(workflowIds: Seq[String], user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[ResponseWorkflows](user, Map(ResourcePol -> View)) {
      Try(workflowService.findByIdList(workflowIds))
    }

  def findByName(name: String, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[ResponseWorkflow](user, Map(ResourcePol -> View)) {
      Try(workflowService.findByName(name))
    }

  def create(workflow: Workflow, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[ResponseWorkflow](user, Map(ResourcePol -> Create, ResourceContext -> Create)) {
      Try(workflowService.create(workflow))
    }

  def createList(workflows: Seq[Workflow], user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[ResponseWorkflows](user, Map(ResourcePol -> Create, ResourceContext -> Create)) {
      Try(workflowService.createList(workflows))
    }

  def update(workflow: Workflow, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer(user, Map(ResourcePol -> Edit)) {
      Try(workflowService.update(workflow)).recover {
        case _: NoNodeException =>
          throw new ServerException(s"No workflow with name ${workflow.name}.")
      }
    }

  def updateList(workflows: Seq[Workflow], user: Option[LoggedUser]): Unit =
    securityActionAuthorizer(user, Map(ResourcePol -> Edit)) {
      Try(workflowService.updateList(workflows))
    }

  def delete(id: String, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[Response](user, Map(ResourcePol -> Delete, ResourceCP -> Delete)) {
      workflowService.delete(id)
    }

  def deleteList(workflowIds: Seq[String], user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[Response](user,
      Map(ResourcePol -> Delete, ResourceContext -> Delete, ResourceCP -> Delete)) {
      workflowService.deleteList(workflowIds)
    }

  def deleteAll(user: Option[LoggedUser]): Unit = {
    val actions = Map(ResourcePol -> Delete, ResourceContext -> Delete, ResourceCP -> Delete)
    securityActionAuthorizer[Response](user, actions) {
      workflowService.deleteAll()
    }
  }

  def deleteCheckpoint(name: String, user: Option[LoggedUser]): Unit =
    securityActionAuthorizer[Response](user, Map(ResourceCP -> Delete, ResourcePol -> View)) {
      Try(deleteCheckpointPath(workflowService.findByName(name)))
    }
}

object WorkflowActor extends SLF4JLogging {

  case class CreateWorkflow(workflow: Workflow, user: Option[LoggedUser])

  case class CreateWorkflows(workflows: Seq[Workflow], user: Option[LoggedUser])

  case class Update(workflow: Workflow, user: Option[LoggedUser])

  case class UpdateList(workflows: Seq[Workflow], user: Option[LoggedUser])

  case class DeleteWorkflow(id: String, user: Option[LoggedUser])

  case class DeleteAll(user: Option[LoggedUser])

  case class DeleteList(workflowIds: Seq[String], user: Option[LoggedUser])

  case class FindAll(user: Option[LoggedUser])

  case class Find(id: String, user: Option[LoggedUser])

  case class FindByIdList(workflowIds: Seq[String], user: Option[LoggedUser])

  case class FindByName(name: String, user: Option[LoggedUser])

  case class FindByTemplateType(templateType: String, user: Option[LoggedUser])

  case class FindByTemplateName(templateType: String, name: String, user: Option[LoggedUser])

  case class DeleteCheckpoint(name: String, user: Option[LoggedUser])

  type Response = Try[Unit]

  type ResponseWorkflows = Try[Seq[Workflow]]

  type ResponseWorkflow = Try[Workflow]

}