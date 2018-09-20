/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.sparta.serving.core.services.dao

import java.util.UUID

import com.stratio.sparta.core.models.WorkflowError
import com.stratio.sparta.core.properties.ValidatingPropertyMap._
import com.stratio.sparta.serving.core.constants.SparkConstant.{SubmitAppNameConf, SubmitUiProxyPrefix}
import com.stratio.sparta.serving.core.constants.{AppConstant, MarathonConstant}
import com.stratio.sparta.serving.core.dao.WorkflowExecutionDao
import com.stratio.sparta.serving.core.exception.ServerException
import com.stratio.sparta.serving.core.factory.CuratorFactoryHolder
import com.stratio.sparta.serving.core.helpers.WorkflowHelper
import com.stratio.sparta.serving.core.models.enumerators.WorkflowExecutionMode._
import com.stratio.sparta.serving.core.models.enumerators.WorkflowStatusEnum
import com.stratio.sparta.serving.core.models.enumerators.WorkflowStatusEnum._
import com.stratio.sparta.serving.core.models.workflow._
import com.stratio.sparta.serving.core.services.SparkSubmitService.ExecutionIdKey
import com.stratio.sparta.serving.core.utils.{JdbcSlickConnection, NginxUtils}
import org.joda.time.DateTime
import org.json4s.jackson.Serialization.write
import slick.jdbc.PostgresProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Properties, Success, Try}
import DtoModelImplicits._
import com.github.nscala_time.time.OrderingImplicits._

//scalastyle:off
class WorkflowExecutionPostgresDao extends WorkflowExecutionDao {

  override val profile = PostgresProfile
  override val db = JdbcSlickConnection.db

  import profile.api._

  def findAllExecutions(): Future[List[WorkflowExecution]] = findAll()

  def createDashboardView(): Future[DashboardView] = findAll().map(getDashboardView)

  def findExecutionById(id: String): Future[WorkflowExecution] = findByIdHead(id)

  def createExecution(execution: WorkflowExecution): Future[WorkflowExecution] = {
    log.debug("Adding extra data to execution")
    val executionWithExtraData = addLastUpdateDate(
      addCreatedStatus(
        addMarathonData(
          addSparkSubmitData(
            addWorkflowId(
              addExecutionId(execution))))))

    createAndReturn(executionWithExtraData)
  }

  def updateExecution(execution: WorkflowExecution): Future[WorkflowExecution] =
    for {
      _ <- findByIdHead(execution.id.get)
    } yield {
      upsert(execution)
      execution
    }

  def updateStatus(executionStatus: ExecutionStatusUpdate): Future[WorkflowExecution] = {
    findExecutionById(executionStatus.id).flatMap { actualExecutionStatus =>
      val actualStatus = actualExecutionStatus.lastStatus
      val inputExecutionStatus = executionStatus.status
      val newState = {
        if (inputExecutionStatus.state == WorkflowStatusEnum.NotDefined)
          actualStatus.state
        else inputExecutionStatus.state
      }
      val newStatusInfo = {
        if (inputExecutionStatus.statusInfo.isEmpty && newState != actualStatus.state)
          Option(s"Execution state changed to $newState")
        else if (inputExecutionStatus.statusInfo.isEmpty && newState == actualStatus.state)
          actualStatus.statusInfo
        else inputExecutionStatus.statusInfo
      }
      val newLastUpdateDate = {
        if (newState != actualStatus.state)
          getNewUpdateDate
        else actualStatus.lastUpdateDate
      }
      val newStatus = inputExecutionStatus.copy(
        state = newState,
        statusInfo = newStatusInfo,
        lastUpdateDate = newLastUpdateDate
      )
      val newStatuses = if (newStatus.state == actualStatus.state) {
        newStatus +: actualExecutionStatus.statuses.drop(1)
      } else newStatus +: actualExecutionStatus.statuses
      val newExecutionWithStatus = actualExecutionStatus.copy(statuses = newStatuses)
      val newExInformation = new StringBuilder
      if (actualStatus.state != newStatus.state)
        newExInformation.append(s"\tStatus -> ${actualStatus.state} to ${newStatus.state}")
      if (actualStatus.statusInfo != newStatus.statusInfo)
        newExInformation.append(s"\tInfo -> ${newStatus.statusInfo.getOrElse("No status information registered")}")
      if (newExInformation.nonEmpty)
        log.info(s"Updating execution ${actualExecutionStatus.getExecutionId}: $newExInformation")
      val result = upsert(newExecutionWithStatus).map(_ => newExecutionWithStatus)

      result.onSuccess { case workflowExecution =>
        writeExecutionStatusInZk(WorkflowExecutionStatusChange(actualExecutionStatus, workflowExecution))
      }

      result
    }
  }

  def stopExecution(id: String): Future[WorkflowExecution] = {
    log.debug(s"Stopping workflow execution with id $id")
    updateStatus(ExecutionStatusUpdate(id, ExecutionStatus(WorkflowStatusEnum.Stopping)))
  }

  def deleteAllExecutions(): Future[Boolean] =
    for {
      executions <- findAllExecutions()
    } yield deleteYield(executions)


  def deleteExecution(id: String): Future[Boolean] =
    for {
      execution <- findByIdHead(id)
    } yield deleteYield(Seq(execution))

  def setLaunchDate(executionId: String, date: DateTime): Future[WorkflowExecution] =
    for {
      execution <- findByIdHead(executionId)
      executionUpdated = execution.copy(
        genericDataExecution = execution.genericDataExecution.copy(launchDate = Option(date))
      )
      _ <- db.run(
        table.filter(_.id === executionId)
          .map(execution => execution)
          .update(executionUpdated)
          .transactionally
      )
    } yield executionUpdated

  def setStartDate(executionId: String, date: DateTime): Future[WorkflowExecution] =
    for {
      execution <- findByIdHead(executionId)
      executionUpdated = execution.copy(
        genericDataExecution = execution.genericDataExecution.copy(startDate = Option(date))
      )
      _ <- db.run(
        table.filter(_.id === executionId)
          .map(execution => execution)
          .update(executionUpdated)
          .transactionally
      )
    } yield executionUpdated

  def setEndDate(executionId: String, date: DateTime): Future[WorkflowExecution] =
    for {
      execution <- findByIdHead(executionId)
      executionUpdated = execution.copy(
        genericDataExecution = execution.genericDataExecution.copy(endDate = Option(date))
      )
      _ <- db.run(
        table.filter(_.id === executionId)
          .map(execution => execution)
          .update(executionUpdated)
          .transactionally
      )
    } yield executionUpdated

  def setLastError(executionId: String, error: WorkflowError): Future[WorkflowExecution] =
    for {
      execution <- findByIdHead(executionId)
      executionUpdated = execution.copy(
        genericDataExecution = execution.genericDataExecution.copy(lastError = Option(error))
      )
      _ <- db.run(
        table.filter(_.id === executionId)
          .map(execution => execution)
          .update(executionUpdated)
          .transactionally
      )
    } yield executionUpdated

  def clearLastError(executionId: String): Future[WorkflowExecution] =
    for {
      execution <- findByIdHead(executionId)
      executionUpdated = execution.copy(
        genericDataExecution = execution.genericDataExecution.copy(lastError = None)
      )
      _ <- db.run(
        table.filter(_.id === executionId)
          .map(execution => execution)
          .update(executionUpdated)
          .transactionally
      )
    } yield executionUpdated

  def anyLocalWorkflowRunning: Future[Boolean] =
    for {
      executions <- findAll()
    } yield {
      val executionsRunning = executions.filter(execution =>
        execution.genericDataExecution.executionMode == local && execution.lastStatus.state == Started)
      executionsRunning.nonEmpty
    }

  /** PRIVATE METHODS */

  private[services] def deleteYield(executions: Seq[WorkflowExecution]): Boolean = {
    executions.foreach { execution =>
      log.debug(s"Deleting execution ${execution.id.get}")
      Try(deleteByID(execution.id.get)) match {
        case Success(_) =>
          log.info(s"Execution with id=${execution.id.get} deleted")
        case Failure(e) =>
          throw e
      }
    }
    true
  }

  private def writeExecutionStatusInZk(workflowExecutionStatusChange: WorkflowExecutionStatusChange): Unit = {

    import AppConstant._
    import workflowExecutionStatusChange._

    Try {
      if (CuratorFactoryHolder.existsPath(ExecutionsStatusChangesZkPath))
        CuratorFactoryHolder.getInstance().setData()
          .forPath(ExecutionsStatusChangesZkPath, write(workflowExecutionStatusChange).getBytes)
      else CuratorFactoryHolder.getInstance().create().creatingParentsIfNeeded()
        .forPath(ExecutionsStatusChangesZkPath, write(workflowExecutionStatusChange).getBytes)
    } match {
      case Success(_) =>
        log.info(s"Execution change notified in Zookeeper for execution ${newExecution.getExecutionId}" +
          s" with last status ${originalExecution.lastStatus.state} and new status ${newExecution.lastStatus.state}")
      case Failure(e) =>
        log.error(s"Error notifying execution change in Zookeeper for execution ${newExecution.getExecutionId}" +
          s" with last status ${originalExecution.lastStatus.state} and new status ${newExecution.lastStatus.state}", e)
        CuratorFactoryHolder.resetInstance()
        throw e
    }
  }

  private[services] def findByIdHead(id: String): Future[WorkflowExecution] =
    for {
      workflowExecList <- filterByIdReal(id)
    } yield {
      if (workflowExecList.nonEmpty)
        workflowExecList.head
      else throw new ServerException(s"No Workflow Execution found with id $id")
    }

  private[services] def filterByIdReal(id: String): Future[Seq[WorkflowExecution]] =
    db.run(table.filter(_.id === id).result)

  private[services] def addId(execution: WorkflowExecution, newId: String): WorkflowExecution = {
    execution.copy(id = Option(newId))
  }

  private[services] def addWorkflowId(execution: WorkflowExecution): WorkflowExecution = {
    val newGenericDataExecution = execution.genericDataExecution.copy(
      workflow = execution.genericDataExecution.workflow.copy(executionId = execution.id)
    )

    execution.copy(genericDataExecution = newGenericDataExecution)
  }

  private[services] def addMarathonData(execution: WorkflowExecution): WorkflowExecution = {
    if (execution.genericDataExecution.executionMode == marathon) {
      val marathonId = WorkflowHelper.getMarathonId(execution)
      val sparkUri = NginxUtils.buildSparkUI(WorkflowHelper.getExecutionDeploymentId(execution))
      execution.copy(
        marathonExecution = execution.marathonExecution match {
          case Some(mExecution) =>
            Option(mExecution.copy(
              marathonId = marathonId,
              sparkURI = sparkUri
            ))
          case None =>
            Option(MarathonExecution(
              marathonId = marathonId,
              sparkURI = sparkUri
            ))
        }
      )
    } else execution
  }

  private[services] def addSparkSubmitData(execution: WorkflowExecution): WorkflowExecution = {
    val newSparkSubmitExecution = execution.sparkSubmitExecution.map { submitEx =>
      val sparkConfWithNginx = addNginxPrefixConf(execution, submitEx.sparkConfigurations)
      val sparkConfWithName = addAppNameConf(
        sparkConfWithNginx,
        execution.getExecutionId,
        execution.getWorkflowToExecute.name
      )

      submitEx.copy(
        driverArguments = submitEx.driverArguments + (ExecutionIdKey -> execution.getExecutionId),
        sparkConfigurations = sparkConfWithName
      )
    }

    execution.copy(sparkSubmitExecution = newSparkSubmitExecution)
  }

  private[services] def addAppNameConf(
                                        sparkConfs: Map[String, String],
                                        executionId: String,
                                        workflowName: String
                                      ): Map[String, String] =
    if (!sparkConfs.contains(SubmitAppNameConf)) {
      sparkConfs ++ Map(SubmitAppNameConf -> s"$workflowName-$executionId")
    } else sparkConfs

  private[services] def addNginxPrefixConf(
                                            workflowExecution: WorkflowExecution,
                                            sparkConfs: Map[String, String]
                                          ): Map[String, String] = {
    if (Properties.envOrNone(MarathonConstant.NginxMarathonLBHostEnv).notBlank.isDefined &&
      Properties.envOrNone(MarathonConstant.DcosServiceName).notBlank.isDefined) {
      val proxyLocation = WorkflowHelper.getProxyLocation(WorkflowHelper.getExecutionDeploymentId(workflowExecution))
      sparkConfs + (SubmitUiProxyPrefix -> proxyLocation)
    } else sparkConfs
  }

  private[services] def addExecutionId(execution: WorkflowExecution): WorkflowExecution =
    execution.id match {
      case None =>
        addId(execution, UUID.randomUUID.toString)
      case Some(id) =>
        addId(execution, id)
    }

  private[services] def addCreatedStatus(execution: WorkflowExecution): WorkflowExecution =
    execution.copy(statuses = Seq(ExecutionStatus(
      state = Created,
      statusInfo = Option("Workflow execution created correctly")
    )))

  private[services] def addLastUpdateDate(execution: WorkflowExecution): WorkflowExecution = {
    val createDate = getNewUpdateDate
    if (execution.statuses.isEmpty) {
      execution.copy(statuses = Seq(ExecutionStatus(
        state = Created,
        statusInfo = Option("Workflow execution created correctly"),
        lastUpdateDate = createDate
      )))
    } else {
      val newStatuses = execution.statuses.map { executionStatus =>
        executionStatus.copy(
          lastUpdateDate = createDate
        )
      }
      execution.copy(statuses = newStatuses)
    }
  }

  private[services] def getNewUpdateDate: Option[DateTime] = Option(new DateTime())

  private[services] def getDashboardView(executions: Seq[WorkflowExecution]): DashboardView = {
    val lastExecutions = executions.sortBy { execution =>
      import execution.genericDataExecution._
      val genericSettingsDate = launchDate.orElse(startDate).orElse(endDate).getOrElse(new DateTime())
      execution.statuses.headOption match {
        case Some(status) => status.lastUpdateDate.getOrElse(genericSettingsDate)
        case None => genericSettingsDate
      }
    }.takeRight(10).reverse.map { execution =>
      val executionDto: WorkflowExecutionDto = execution
      executionDto
    }
    val running = executions.count { execution =>
      execution.statuses.headOption match {
        case Some(status) =>
          Seq(Launched, Starting, Started, Uploaded).contains(status.state)
        case None =>
          false
      }
    }
    val stopped = executions.count { execution =>
      execution.statuses.headOption match {
        case Some(status) =>
          Seq(Stopping, Stopped, Finished, NotDefined, Created, NotStarted, Killed).contains(status.state)
        case None =>
          false
      }
    }
    val failed = executions.count { execution =>
      execution.statuses.headOption match {
        case Some(status) =>
          Seq(Failed).contains(status.state)
        case None =>
          false
      }
    }
    val archived = 0
    val summary = ExecutionsSummary(running, stopped, failed, archived)

    DashboardView(lastExecutions, summary)
  }
}