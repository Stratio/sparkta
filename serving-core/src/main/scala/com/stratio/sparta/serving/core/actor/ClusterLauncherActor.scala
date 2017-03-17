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

import akka.actor.{Actor, Cancellable, PoisonPill}
import com.stratio.sparta.serving.core.actor.LauncherActor.{Start, StartWithRequest}
import com.stratio.sparta.serving.core.config.SpartaConfig
import com.stratio.sparta.serving.core.constants.AppConstant._
import com.stratio.sparta.serving.core.models.enumerators.PolicyStatusEnum._
import com.stratio.sparta.serving.core.models.policy.{PhaseEnum, PolicyErrorModel, PolicyModel, PolicyStatusModel}
import com.stratio.sparta.serving.core.models.submit.SubmitRequest
import com.stratio.sparta.serving.core.services.ClusterCheckerService
import com.stratio.sparta.serving.core.utils.{ClusterListenerUtils, LauncherUtils, PolicyStatusUtils, RequestUtils, SchedulerUtils, SparkSubmitUtils}
import org.apache.curator.framework.CuratorFramework
import org.apache.spark.launcher.SparkLauncher

import scala.util.{Failure, Success, Try}

class ClusterLauncherActor(val curatorFramework: CuratorFramework) extends Actor
  with SchedulerUtils with SparkSubmitUtils with PolicyStatusUtils
  with ClusterListenerUtils with LauncherUtils with RequestUtils {

  private val clusterCheckerService = new ClusterCheckerService(curatorFramework)
  private val checkersPolicyStatus = scala.collection.mutable.ArrayBuffer.empty[Cancellable]

  override def receive: PartialFunction[Any, Unit] = {
    case Start(policy: PolicyModel) => initializeSubmitRequest(policy)
    case StartWithRequest(policy: PolicyModel, submitRequest: SubmitRequest) => launch(policy, submitRequest)
    case _ => log.info("Unrecognized message in Cluster Launcher Actor")
  }

  override def postStop(): Unit = checkersPolicyStatus.foreach(_.cancel())

  def initializeSubmitRequest(policy: PolicyModel): Unit = {
    Try {
      log.info(s"Initializing cluster submit options from policy: ${policy.name}")
      val zookeeperConfig = getZookeeperConfig
      val execMode = executionMode(policy)
      val clusterConfig = SpartaConfig.getClusterConfig(Option(execMode)).get
      val detailExecMode = getDetailExecutionMode(policy, clusterConfig)
      val sparkHome = validateSparkHome(clusterConfig)
      val driverFile = extractDriverClusterSubmit(policy, DetailConfig, SpartaConfig.getHdfsConfig)
      val master = clusterConfig.getString(Master).trim
      val pluginsFiles = pluginsJars(policy)
      val driverArguments =
        extractDriverArguments(policy, driverFile, clusterConfig, zookeeperConfig, execMode, pluginsFiles)
      val (sparkSubmitArguments, sparkConfigurations) =
        extractSubmitArgumentsAndSparkConf(policy, clusterConfig, pluginsFiles)
      val submitRequest = SubmitRequest(policy.id.get, SpartaDriverClass, driverFile, master, sparkSubmitArguments,
        sparkConfigurations, driverArguments, detailExecMode, killUrl(clusterConfig), Option(sparkHome))

      createRequest(submitRequest)
    } match {
      case Failure(exception) =>
        val information = s"Error when initializing the Sparta submit options"
        log.error(information, exception)
        updateStatus(PolicyStatusModel(id = policy.id.get, status = Failed, statusInfo = Option(information),
          lastError = Option(PolicyErrorModel(information, PhaseEnum.Execution, exception.toString))))
        self ! PoisonPill
      case Success(Failure(exception)) =>
        val information = s"Error when creating submit request in the persistence "
        log.error(information, exception)
        updateStatus(PolicyStatusModel(id = policy.id.get, status = Failed, statusInfo = Option(information),
          lastError = Option(PolicyErrorModel(information, PhaseEnum.Execution, exception.toString))
        ))
        self ! PoisonPill
      case Success(Success(submitRequestCreated)) =>
        val information = "Sparta submit options initialized correctly"
        log.info(information)
        updateStatus(PolicyStatusModel(id = policy.id.get, status = NotStarted,
          statusInfo = Option(information), lastExecutionMode = Option(submitRequestCreated.executionMode)))

        launch(policy, submitRequestCreated)
    }
  }

  def launch(policy: PolicyModel, submitRequest: SubmitRequest): Unit = {
    Try {
      log.info(s"Launching Sparta Job with options ... \n\tPolicy name: ${policy.name}\n\t" +
        s"Main Class: $SpartaDriverClass\n\tDriver file: ${submitRequest.driverFile}\n\t" +
        s"Master: ${submitRequest.master}\n\tSpark submit arguments: ${submitRequest.submitArguments.mkString(",")}" +
        s"\n\tSpark configurations: ${submitRequest.sparkConfigurations.mkString(",")}\n\t" +
        s"Driver arguments: ${submitRequest.driverArguments}")
      val sparkLauncher = new SparkLauncher()
        .setAppResource(submitRequest.driverFile)
        .setMainClass(submitRequest.driverClass)
        .setMaster(submitRequest.master)

      //Set Spark Home
      submitRequest.sparkHome.foreach(home => sparkLauncher.setSparkHome(home))
      //Spark arguments
      submitRequest.submitArguments.filter(_._2.nonEmpty)
        .foreach { case (k: String, v: String) => sparkLauncher.addSparkArg(k, v) }
      submitRequest.submitArguments.filter(_._2.isEmpty)
        .foreach { case (k: String, v: String) => sparkLauncher.addSparkArg(k) }
      // Spark properties
      submitRequest.sparkConfigurations.filter(_._2.nonEmpty)
        .foreach { case (key: String, value: String) => sparkLauncher.setConf(key.trim, value.trim) }
      // Driver (Sparta) params
      submitRequest.driverArguments.toSeq.sortWith { case (a, b) => a._1 < b._1 }
        .foreach { case (_, argValue) => sparkLauncher.addAppArgs(argValue) }
      // Launch SparkApp
      sparkLauncher.startApplication(addSparkListener(policy))
    } match {
      case Failure(exception) =>
        val information = s"Error when launching the Sparta cluster job"
        log.error(information, exception)
        updateStatus(PolicyStatusModel(id = policy.id.get, status = Failed, statusInfo = Option(information),
          lastError = Option(PolicyErrorModel(information, PhaseEnum.Execution, exception.toString))
        ))
        self ! PoisonPill
      case Success(sparkHandler) =>
        val information = "Sparta cluster job launched correctly"
        log.info(information)
        updateStatus(PolicyStatusModel(id = policy.id.get, status = Launched,
          submissionId = Option(sparkHandler.getAppId), submissionStatus = Option(sparkHandler.getState.name()),
          statusInfo = Option(information)))
        if (submitRequest.executionMode.contains(ClusterValue))
          addClusterContextListener(policy.id.get, policy.name, submitRequest.killUrl, Option(self), Option(context))
        else addClientContextListener(policy.id.get, policy.name, sparkHandler, self, context)
        checkersPolicyStatus += scheduleOneTask(AwaitPolicyChangeStatus, DefaultAwaitPolicyChangeStatus)(
          clusterCheckerService.checkPolicyStatus(policy, self, context))
    }
  }
}