/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.sparta.dg.agent.commons

import com.stratio.sparta.core.ContextBuilder.ContextBuilderImplicits
import com.stratio.sparta.core.constants.SdkConstants._
import com.stratio.sparta.core.helpers.SdkSchemaHelper
import com.stratio.sparta.core.properties.ValidatingPropertyMap._
import com.stratio.sparta.core.constants.SdkConstants._
import com.stratio.sparta.core.workflow.step.{InputStep, OutputStep}
import com.stratio.sparta.dg.agent.models.LineageWorkflow
import com.stratio.sparta.serving.core.constants.AppConstant
import com.stratio.sparta.serving.core.constants.AppConstant.defaultWorkflowRelationSettings
import com.stratio.sparta.serving.core.error.PostgresNotificationManagerImpl
import com.stratio.sparta.serving.core.helpers.GraphHelper.createGraph
import com.stratio.sparta.serving.core.models.enumerators.WorkflowExecutionEngine._
import com.stratio.sparta.serving.core.models.enumerators.WorkflowStatusEnum._
import com.stratio.sparta.serving.core.models.enumerators.{DataType, WorkflowStatusEnum}
import com.stratio.sparta.serving.core.models.workflow._
import com.stratio.sparta.serving.core.workflow.SpartaWorkflow
import org.apache.spark.sql.Dataset
import org.apache.spark.streaming.dstream.DStream

import scala.util.{Properties, Try}
import scalax.collection._
import scalax.collection.edge.LDiEdge
import scala.util.{Properties, Try}

//scalastyle:off
object LineageUtils extends ContextBuilderImplicits {

  case class OutputNodeLineageRelation(outputName: String, nodeTableName: String, outputClassPrettyName: String, outputStepType: String)

  val StartKey = "startedAt"
  val FinishedKey = "finishedAt"
  val TypeFinishedKey = "detailedStatus"
  val ErrorKey = "error"
  val UrlKey = "link"
  val PublicSchema = "public"

  lazy val spartaVHost = AppConstant.virtualHost.getOrElse("localhost")

  def checkIfProcessableWorkflow(executionStatusChange: WorkflowExecutionStatusChange): Boolean = {
    val eventStatus = executionStatusChange.newExecution.lastStatus.state
    val exEngine = executionStatusChange.newExecution.executionEngine.get

    (exEngine == Batch && eventStatus == WorkflowStatusEnum.Finished || eventStatus == WorkflowStatusEnum.Failed
      || eventStatus == WorkflowStatusEnum.StoppedByUser) ||
      (exEngine == Streaming && eventStatus == WorkflowStatusEnum.Started || eventStatus == WorkflowStatusEnum.StoppedByUser
        || eventStatus == WorkflowStatusEnum.Finished || eventStatus == WorkflowStatusEnum.Failed)
  }

  def getOutputNodesWithWriter(workflow: Workflow): Seq[OutputNodeLineageRelation] = {
    import com.stratio.sparta.serving.core.helpers.GraphHelperImplicits._

    val graph: Graph[NodeGraph, LDiEdge] = createGraph(workflow)

    workflow.pipelineGraph.nodes.filter(_.stepType.toLowerCase == OutputStep.StepType)
      .sorted
      .flatMap { outputNode =>
        val outNodeGraph = graph.get(outputNode)
        val predecessors = outNodeGraph.diPredecessors.toList
        predecessors.map { node =>
          val tableName = {
            val relationSettings = Try {
              node.findOutgoingTo(outNodeGraph).get.value.edge.label.asInstanceOf[WorkflowRelationSettings]
            }.getOrElse(defaultWorkflowRelationSettings)

            if (relationSettings.dataType == DataType.ValidData)
              node.writer.tableName.notBlank.getOrElse(node.name)
            else if (relationSettings.dataType == DataType.DiscardedData)
              node.writer.discardTableName.notBlank.getOrElse(SdkSchemaHelper.discardTableName(node.name))
            else node.name
          }

          OutputNodeLineageRelation(outNodeGraph.name, tableName, outNodeGraph.classPrettyName, outputNode.stepType)
        }
      }
  }

  def getAllStepsProperties(workflow: Workflow): Map[String, Map[String, String]] = {

    val errorManager = PostgresNotificationManagerImpl(workflow)
    val inOutNodes = workflow.pipelineGraph.nodes.filter(node =>
      node.stepType.toLowerCase == OutputStep.StepType || node.stepType.toLowerCase == InputStep.StepType).map(_.name)

    if (workflow.executionEngine == Streaming) {
      val spartaWorkflow = SpartaWorkflow[DStream](workflow, errorManager)
      spartaWorkflow.stages(execute = false)
      spartaWorkflow.lineageProperties(inOutNodes)
    } else if (workflow.executionEngine == Batch) {
      val spartaWorkflow = SpartaWorkflow[Dataset](workflow, errorManager)
      spartaWorkflow.stages(execute = false)
      spartaWorkflow.lineageProperties(inOutNodes)
    } else Map.empty
  }

  def setExecutionUrl(executionId: String): String = {
    "https://" + spartaVHost + "/" + AppConstant.spartaTenant + "/#/executions/" + executionId
  }

  def setExecutionProperties(newExecution: WorkflowExecution): Map[String, String] = {
    Map(
      StartKey -> newExecution.genericDataExecution.startDate.getOrElse(None).toString,
      FinishedKey -> newExecution.resumedDate.getOrElse(None).toString,
      TypeFinishedKey -> Try(newExecution.statuses.head.state.toString).getOrElse("Finished"),
      ErrorKey -> newExecution.genericDataExecution.lastError.toString,
      UrlKey -> setExecutionUrl(newExecution.getExecutionId))
  }

  def updateLineageWorkflow(responseWorkflow: LineageWorkflow, newWorkflow: LineageWorkflow): LineageWorkflow = {
    LineageWorkflow(
      id = responseWorkflow.id,
      name = responseWorkflow.name,
      description = responseWorkflow.description,
      tenant = responseWorkflow.tenant,
      properties = newWorkflow.properties,
      transactionId = responseWorkflow.transactionId,
      actorType = responseWorkflow.actorType,
      jobType = responseWorkflow.jobType,
      statusCode = newWorkflow.statusCode,
      version = responseWorkflow.version,
      listActorMetaData = responseWorkflow.listActorMetaData
    )
  }


  def addTableNameFromWriterToOutput(nodesOutGraph: Seq[OutputNodeLineageRelation],
                                     lineageProperties: Map[String, Map[String, String]]): Seq[(String, Map[String, String])] = {
    nodesOutGraph.map { outputNodeLineageRelation =>
      import outputNodeLineageRelation._
       val newProperties = {
         val outputProperties = lineageProperties.getOrElse(outputName, Map.empty)
         val sourceProperty = outputProperties.get(SourceKey)
         val schema = outputProperties.getOrElse(DefaultSchemaKey, PublicSchema)

         outputProperties.map { case property@(key, value) =>
           if (key.equals(ResourceKey) && isFileSystemStepType(outputClassPrettyName))
             (ResourceKey, nodeTableName)
           else if (key.equals(ResourceKey) && isJdbcStepType(outputClassPrettyName))
             if(!nodeTableName.contains(".") && sourceProperty.isDefined && sourceProperty.get.toLowerCase.contains("postgres")) {
               (ResourceKey, s"$schema.$nodeTableName")
             } else (ResourceKey, nodeTableName)
           else property
         }
       }

      outputName -> newProperties
    }
  }



  def extraPathFromFilesystemOutput(stepType: String, stepClass: String, path: Option[String],
                                    resource: Option[String]): String =
    if (stepType.equals(OutputStep.StepType) && isFileSystemStepType(stepClass) && resource.nonEmpty) {
      "/" + resource.getOrElse("")
    }
    else ""

  def mapSparta2GovernanceJobType(executionEngine: ExecutionEngine): String =
    executionEngine match {
      case Streaming => "STREAM"
      case Batch => "BATCH"
    }

  def mapSparta2GovernanceStepType(stepType: String): String =
    stepType match {
      case InputStep.StepType => "IN"
      case OutputStep.StepType => "OUT"
    }

  def mapSparta2GovernanceStatuses(spartaStatus: WorkflowStatusEnum.Value): String =
    spartaStatus match {
      case Started => "RUNNING"
      case Finished => "FINISHED"
      case Failed => "ERROR"
      case StoppedByUser => "FINISHED"
    }

  def isFileSystemStepType(dataStoreType: String): Boolean =
    dataStoreType match {
      case "Avro" | "Csv" | "FileSystem" | "Parquet" | "Xml" | "Json" | "Text" => true
      case _ => false
    }

  def isJdbcStepType(dataStoreType: String): Boolean =
    dataStoreType match {
      case "Jdbc" | "Postgres" => true
      case _ => false
    }

  def mapSparta2GovernanceDataStoreType(dataStoreType: String): String =
    dataStoreType match {
      case "Avro" | "Csv" | "FileSystem" | "Parquet" | "Xml" | "Json" | "Text" => "HDFS"
      case "Jdbc" | "Postgres" => "SQL"
    }
}