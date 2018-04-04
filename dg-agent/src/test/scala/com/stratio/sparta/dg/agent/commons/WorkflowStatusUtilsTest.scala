/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.dg.agent.commons

import com.stratio.governance.commons.agent.model.metadata.MetadataPath
import com.stratio.governance.commons.agent.model.metadata.lineage.EventType
import com.stratio.sparta.dg.agent.model.SpartaWorkflowStatusMetadata
import com.stratio.sparta.sdk.properties.JsoneyString
import com.stratio.sparta.serving.core.models.enumerators.{NodeArityEnum, WorkflowStatusEnum}
import com.stratio.sparta.serving.core.models.workflow._
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorkflowStatusUtilsTest extends WordSpec with Matchers {
  val nodes = Seq(
    NodeGraph("a", "Input", "", "", Seq(NodeArityEnum.NullaryToNary), WriterGraph()),
    NodeGraph("b", "Output", "", "", Seq(NodeArityEnum.NaryToNullary), WriterGraph())
  )
  val edges = Seq(
    EdgeGraph("a", "b")
  )
  val validPipeGraph = PipelineGraph(nodes , edges)
  val emptyPipeGraph = PipelineGraph(Seq.empty[NodeGraph], Seq.empty[EdgeGraph])
  val settingsModel = Settings(
    GlobalSettings("local", Seq.empty, Seq.empty, true ,Some(JsoneyString("constraint1:constraint2"))),
    StreamingSettings(
      JsoneyString("6s"), None, None, None, None, None, None, None, CheckpointSettings(JsoneyString("test/test"))),
    SparkSettings(
      JsoneyString("local[*]"), sparkKerberos = false, sparkDataStoreTls = false,
      sparkMesosSecurity = false, None, SubmitArguments(), SparkConf(SparkResourcesConf()))
  )

  val timestampEpochTest = 1519051473L
  val pipeline = PipelineGraph(nodes , edges)
  val testWorkflow = Workflow(Option("qwerty12345"), "kafka-test",
    settings = settingsModel,
    pipelineGraph = pipeline,
    group = Group(Option("987654"), "/home/test/subgroup"),
    lastUpdateDate = Option(new DateTime(timestampEpochTest))
  )

  "WorkflowStatusUtils.statusMetadataLineage" should {
    "return None" when {
      "we receive a transient state event" in {
        val statusEvent: WorkflowStatusStream = WorkflowStatusStream(
          WorkflowStatus("qwerty12345",
            WorkflowStatusEnum.Starting,
            lastUpdateDate = Some(new DateTime(timestampEpochTest))),
          Some(testWorkflow),
          None
        )
        LineageUtils.statusMetadataLineage(statusEvent) should be (None)
      }

      "the event has no workflow info associated to the status" in {
        val statusEvent: WorkflowStatusStream = WorkflowStatusStream(
          WorkflowStatus("qwerty12345", WorkflowStatusEnum.Finished),
          None,
          None
        )
        LineageUtils.statusMetadataLineage(statusEvent) should be (None)
      }
    }

    "return a List[SpartaWorkflowStatusMetadata]" in {
      val statusEvent: WorkflowStatusStream = WorkflowStatusStream(
        WorkflowStatus("qwerty12345", WorkflowStatusEnum.Finished,
          lastUpdateDate = Option(new DateTime(timestampEpochTest)),
          lastUpdateDateWorkflow = Option(new DateTime(timestampEpochTest))),
        Some(testWorkflow),
        None
      )
      val metadataPath =  MetadataPath(Seq("sparta",
        "home_test_subgroup_kafka-test_0",
        "1519051473",
        "1519051473"))

      val expected = SpartaWorkflowStatusMetadata("kafka-test",
        EventType.Success,
        Some(""),
        "qwerty12345",
        metadataPath,
        tags = List.empty[String],
        modificationTime = Option(timestampEpochTest),
        accessTime = Option(timestampEpochTest))

      LineageUtils.statusMetadataLineage(statusEvent) should equal (Some(List(expected)))
    }
  }


}
