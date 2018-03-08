/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.api.actor

import scala.util.{Failure, Success, Try}

import akka.actor.{Props, _}
import org.apache.curator.framework.CuratorFramework

import com.stratio.sparta.security.{Edit, SpartaSecurityManager}
import com.stratio.sparta.serving.core.actor.ClusterLauncherActor
import com.stratio.sparta.serving.core.actor.LauncherActor.{Launch, Start}
import com.stratio.sparta.serving.core.constants.AkkaConstant._
import com.stratio.sparta.serving.core.constants.{AkkaConstant, AppConstant}
import com.stratio.sparta.serving.core.models.dto.LoggedUser
import com.stratio.sparta.serving.core.models.enumerators.WorkflowStatusEnum.Failed
import com.stratio.sparta.serving.core.models.workflow.{PhaseEnum, WorkflowError, WorkflowStatus}
import com.stratio.sparta.serving.core.services.{WorkflowService, WorkflowStatusService}
import com.stratio.sparta.serving.core.utils.ActionUserAuthorize

class LauncherActor(curatorFramework: CuratorFramework,
                    statusListenerActor: ActorRef,
                    envStateActor: ActorRef
                   )(implicit val secManagerOpt: Option[SpartaSecurityManager])
  extends Actor with ActionUserAuthorize {

  private val ResourceStatus = "status"
  private val statusService = new WorkflowStatusService(curatorFramework)
  private val workflowService = new WorkflowService(curatorFramework, Option(context.system), Option(envStateActor))

  private val marathonLauncherActor = context.actorOf(Props(
    new MarathonLauncherActor(curatorFramework, statusListenerActor)), MarathonLauncherActorName)
  private val clusterLauncherActor = context.actorOf(Props(
    new ClusterLauncherActor(curatorFramework, statusListenerActor)), ClusterLauncherActorName)

  override def receive: Receive = {
    case Launch(id, user) => launch(id, user)
    case _ => log.info("Unrecognized message in Launcher Actor")
  }

  def launch(id: String, user: Option[LoggedUser]): Unit = {
    securityActionAuthorizer(user, Map(ResourceStatus -> Edit)) {
      Try {
        val workflow = workflowService.findById(id)
        val workflowLauncherActor = workflow.settings.global.executionMode match {
          case AppConstant.ConfigMarathon =>
            log.info(s"Launching workflow: ${workflow.name} in marathon mode")
            marathonLauncherActor
          case AppConstant.ConfigMesos =>
            log.info(s"Launching workflow: ${workflow.name} in cluster mode")
            clusterLauncherActor
          case AppConstant.ConfigLocal if !statusService.isAnyLocalWorkflowStarted =>
            val actorName = AkkaConstant.cleanActorName(s"LauncherActor-${workflow.name}")
            val childLauncherActor = context.children.find(children => children.path.name == actorName)
            log.info(s"Launching workflow: ${workflow.name} in local mode")
            childLauncherActor.getOrElse(context.actorOf(Props(
              new LocalLauncherActor(statusListenerActor,curatorFramework)), actorName))
          case _ =>
            throw new Exception(
              s"Invalid execution mode in workflow ${workflow.name}: ${workflow.settings.global.executionMode}")
        }

        workflowLauncherActor ! Start(workflow)
        (workflow, workflowLauncherActor)
      } match {
        case Success((workflow, launcherActor)) =>
          log.debug(s"Workflow ${workflow.name} launched to: ${launcherActor.toString()}")
        case Failure(exception) =>
          val information = s"Error launching workflow with the selected execution mode"
          log.error(information)
          statusService.update(WorkflowStatus(
            id = id,
            status = Failed,
            statusInfo = Option(information),
            lastError = Option(WorkflowError(information, PhaseEnum.Launch, exception.toString))
          ))
      }
    }
  }
}