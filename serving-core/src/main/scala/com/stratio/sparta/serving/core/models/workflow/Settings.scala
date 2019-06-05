/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.core.models.workflow

import com.stratio.sparta.core.models.ErrorsManagement
import com.stratio.sparta.core.properties.JsoneyString
import com.stratio.sparta.serving.core.models.dto.Dto
import com.stratio.sparta.serving.core.models.enumerators.DeployMode.DeployMode
import com.stratio.sparta.serving.core.models.enumerators.WorkflowExecutionMode.WorkflowExecutionMode
import com.stratio.sparta.serving.core.models.enumerators.{DeployMode, WorkflowExecutionMode}

case class Settings(
                     global: GlobalSettings = GlobalSettings(),
                     streamingSettings: StreamingSettings = StreamingSettings(),
                     sparkSettings: SparkSettings = SparkSettings(),
                     errorsManagement: ErrorsManagement = ErrorsManagement()
                   )

case class GlobalSettings(
                           executionMode: WorkflowExecutionMode = WorkflowExecutionMode.marathon,
                           userPluginsJars: Seq[UserJar] = Seq.empty,
                           preExecutionSqlSentences: Seq[SqlSentence] = Seq.empty,
                           postExecutionSqlSentences: Seq[SqlSentence] = Seq.empty,
                           addAllUploadedPlugins: Boolean = true,
                           mesosConstraint: Option[JsoneyString] = None,
                           mesosConstraintOperator: Option[JsoneyString] = None,
                           parametersLists: Seq[String] = Seq.empty[String],
                           parametersUsed: Seq[String] = Seq.empty[String],
                           udfsToRegister: Seq[UserUDF] = Seq.empty[UserUDF],
                           udafsToRegister: Seq[UserUDF] = Seq.empty[UserUDF],
                           mesosRole: Option[JsoneyString] = None,
                           marathonDeploymentSettings: Option[MarathonDeploymentSettings] = None,
                           enableQualityRules: Option[Boolean] = Option(false)
                         )

case class GlobalSettingsDto(
                              executionMode: WorkflowExecutionMode = WorkflowExecutionMode.marathon,
                              parametersLists: Seq[String] = Seq.empty[String],
                              parametersUsed: Seq[String] = Seq.empty[String]
                            ) extends Dto

case class CheckpointSettings(
                               checkpointPath: JsoneyString = JsoneyString("sparta/checkpoint"),
                               enableCheckpointing: Boolean = true,
                               autoDeleteCheckpoint: Boolean = true,
                               addTimeToCheckpointPath: Boolean = false,
                               keepSameCheckpoint: Option[Boolean] = None
                             )

case class MarathonDeploymentSettings(
                                       gracePeriodSeconds: Option[JsoneyString] = None,
                                       intervalSeconds: Option[JsoneyString] = None,
                                       timeoutSeconds: Option[JsoneyString] = None,
                                       maxConsecutiveFailures: Option[JsoneyString] = None,
                                       forcePullImage: Option[Boolean] = Option(false),
                                       privileged: Option[Boolean] = Option(false),
                                       userEnvVariables: Seq[KeyValuePair] = Seq.empty[KeyValuePair],
                                       userLabels: Seq[KeyValuePair] = Seq.empty[KeyValuePair],
                                       logLevel: Option[JsoneyString] = None
                                     )

case class StreamingSettings(
                              window: JsoneyString = JsoneyString("2s"),
                              remember: Option[JsoneyString] = None,
                              backpressure: Option[Boolean] = None,
                              backpressureInitialRate: Option[JsoneyString] = None,
                              backpressureMaxRate: Option[JsoneyString] = None,
                              blockInterval: Option[JsoneyString] = Option(JsoneyString("100ms")),
                              stopGracefully: Option[Boolean] = None,
                              stopGracefullyTimeout: Option[String] = None,
                              checkpointSettings: CheckpointSettings = CheckpointSettings()
                            )

case class SparkSettings(
                          master: JsoneyString = JsoneyString("mesos://zk://master.mesos:2181/mesos"),
                          sparkKerberos: Boolean = true,
                          sparkDataStoreTls: Boolean = true,
                          sparkMesosSecurity: Boolean = true,
                          killUrl: Option[JsoneyString] = None,
                          submitArguments: SubmitArguments = SubmitArguments(),
                          sparkConf: SparkConf = SparkConf()
                        )

case class SubmitArguments(
                            userArguments: Seq[UserSubmitArgument] = Seq.empty[UserSubmitArgument],
                            deployMode: Option[DeployMode] = Option(DeployMode.client),
                            driverJavaOptions: Option[JsoneyString] = Option(JsoneyString(
                              "-Dconfig.file=/etc/sds/sparta/spark/reference.conf -XX:+UseConcMarkSweepGC -Dlog4j.configurationFile=file:///etc/sds/sparta/log4j2.xml"))
                          )

case class SparkConf(
                      sparkResourcesConf: SparkResourcesConf = SparkResourcesConf(),
                      sparkHistoryServerConf: SparkHistoryServerConf = SparkHistoryServerConf(),
                      userSparkConf: Seq[SparkProperty] = Seq.empty[SparkProperty],
                      coarse: Option[Boolean] = None,
                      sparkUser: Option[JsoneyString] = None,
                      sparkLocalDir: Option[JsoneyString] = None,
                      sparkKryoSerialization: Option[Boolean] = None,
                      sparkSqlCaseSensitive: Option[Boolean] = None,
                      logStagesProgress: Option[Boolean] = None,
                      hdfsTokenCache: Option[Boolean] = None,
                      executorExtraJavaOptions: Option[JsoneyString] = Option(JsoneyString(
                        "-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseConcMarkSweepGC"))
                    )

case class SparkResourcesConf(
                               coresMax: Option[JsoneyString] = Option(JsoneyString("2")),
                               executorMemory: Option[JsoneyString] = Option(JsoneyString("2G")),
                               executorCores: Option[JsoneyString] = Option(JsoneyString("1")),
                               driverCores: Option[JsoneyString] = Option(JsoneyString("1")),
                               driverMemory: Option[JsoneyString] = Option(JsoneyString("2G")),
                               mesosExtraCores: Option[JsoneyString] = None,
                               localityWait: Option[JsoneyString] = Option(JsoneyString("100")),
                               taskMaxFailures: Option[JsoneyString] = Option(JsoneyString("8")),
                               sparkMemoryFraction: Option[JsoneyString] = Option(JsoneyString("0.6")),
                               sparkParallelism: Option[JsoneyString] = None
                             )

case class SparkHistoryServerConf(
                                   enableHistoryServerMonitoring: Boolean = false,
                                   sparkHistoryServerLogDir: Option[JsoneyString] = None,
                                   sparkHistoryServerMonitoringURL: Option[JsoneyString] = None,
                                   sparkHistoryServerEventLogRotateEnable: Option[Boolean] = Option(false),
                                   sparkHistoryServerEventLogRotateNum: Option[JsoneyString] = Option(JsoneyString("9")),
                                   sparkHistoryServerEventLogRotateSize: Option[JsoneyString] = Option(JsoneyString("512"))
                                 )

case class UserSubmitArgument(
                               submitArgument: JsoneyString,
                               submitValue: JsoneyString
                             )

case class UserJar(jarPath: JsoneyString)

case class UserUDF(name: String)

case class SqlSentence(sentence: JsoneyString)

case class SparkProperty(sparkConfKey: JsoneyString, sparkConfValue: JsoneyString)

case class KeyValuePair(key: JsoneyString, value: JsoneyString)
