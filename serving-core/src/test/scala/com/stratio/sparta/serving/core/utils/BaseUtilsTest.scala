/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.core.utils

import akka.actor.ActorSystem
import akka.testkit._
import com.stratio.sparta.sdk.properties.JsoneyString
import com.stratio.sparta.serving.core.config.SpartaConfig
import com.stratio.sparta.serving.core.models.workflow._
import org.apache.curator.framework.CuratorFramework
import org.scalatest._
import org.scalatest.mock.MockitoSugar

abstract class BaseUtilsTest extends TestKit(ActorSystem("UtilsText", SpartaConfig.daemonicAkkaConfig))
  with WordSpecLike
  with Matchers
  with ImplicitSender
  with MockitoSugar {

  SpartaConfig.initMainConfig()
  val curatorFramework = mock[CuratorFramework]
  val interval = 60000

  protected def getWorkflowModel(id: Option[String] = Some("id"),
                                 name: String = "testWorkflow",
                                 executionMode : String = "local"): Workflow = {

    val settingsModel = Settings(
      GlobalSettings(executionMode),
      StreamingSettings(
        JsoneyString("6s"), None, None, None, None, None, None, None, CheckpointSettings()),
      SparkSettings(
        JsoneyString("local[*]"), false, false, false, None, SubmitArguments(),
        SparkConf(SparkResourcesConf()))
    )
    val workflow = Workflow(
      id = id,
      settings = settingsModel,
      name = name,
      description = "whatever",
      pipelineGraph = PipelineGraph(Seq.empty[NodeGraph], Seq.empty[EdgeGraph])
    )
    workflow
  }
}
