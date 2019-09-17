/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.core.constants

import com.stratio.sparta.serving.core.config.SpartaConfig

import scala.util.Try


object AkkaConstant {

  val MasterRole = "spartaMaster"
  val TemplateActorName = "templateActor"
  val WorkflowActorName = "workflowActor"
  val ExecutionActorName = "executionActor"
  val ClusterLauncherActorName = "clusterLauncherActor"
  val LauncherActorName = "launcherActor"
  val MarathonLauncherActorName = "marathonLauncherActor"
  val LocalLauncherActorName = "localLauncherActor"
  val DebugLauncherActorName = "debugLauncherActor"
  val QualityRuleActorName = "qualityRuleActor"
  val PluginActorName = "pluginActor"
  val DriverActorName = "driverActor"
  val ControllerActorName = "controllerActor"
  val MarathonAppActorName = "marathonAppActor"
  val UpDownMarathonActor = "upDownMarathonActor"
  val ConfigActorName = "configurationActor"
  val MetadataActorName = "metadataActor"
  val CrossdataActorName = "crossdataActor"
  val NginxActorName = "nginxActor"
  val GlobalParametersActorName = "globalParametersActor"
  val ParametersStatusListenerActorName = "parametersStatusListenerActor"
  val GroupActorName = "groupActor"
  val DebugWorkflowActorName = "DebugWorkflowApiActor"
  val MlModelsActorName = "MlModelsApiActor"
  val EnvironmentCleanerActorName= "EnvironmentCleanerActor"
  val InconsistentStatusCheckerActorName = "InconsistentStatusCheckerActor"
  val ParameterListActorName = "ParameterListActorName"
  val QualityRuleResultActorName = "qualityRuleResultActor"
  val ScheduledWorkflowTaskActorName = "ScheduledWorkflowTaskActorName"
  val ScheduledWorkflowTaskExecutorActorName = "ScheduledWorkflowTaskExecutorActorName"
  val QualityRuleResultSenderActorName = "QualityRuleResultSenderActor"
  val QualityRuleReceiverActorName = "QualityRuleReceiverActor"
  val JmxMetricsActorName = "JmxMetricsActor"
  val DebugDispatcherActorName = "DebugDispatcherActor"
  val DebugWorkerActorName = "DebugWorkerNode"
  val SchedulerMonitorActorName = "SchedulerMonitorActor"
  val DebugGuardianActorName = "DebugGuardianActor"
  val ValidatorDispatcherActorName = "ValidatorDispatcherActor"
  val ValidatorWorkerActorName = "ValidatorWorkerNode"
  val ValidatorGuardianActorName = "ValidatorGuardianActor"
  val CatalogDispatcherActorName = "CatalogDispatcherActor"
  val CatalogWorkerActorName = "CatalogWorkerNode"
  val CatalogGuardianActorName = "CatalogGuardianActor"
  val ExecutionStatusChangeListenerActorName = "ExecutionStatusChangeListenerActor"
  val ExecutionStatusChangePublisherActorName = "ExecutionStatusChangePublisherActor"
  val RunWorkflowListenerActorName = "RunWorkflowListenerActorName"
  val RunWorkflowPublisherActorName = "RunWorkflowPublisherActor"
  val LineageServiceActorName = "LineageServiceActor"



  lazy val DefaultInstances = Try(SpartaConfig.getDetailConfig().get.getInt("actors.instances"))
    .getOrElse(Runtime.getRuntime.availableProcessors())

  def cleanActorName(initialName: String): String = initialName.replace(" ", "_")
}
