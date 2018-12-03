/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.output.mlpipeline.validation.steps.preprocessing

import java.io.{Serializable => JSerializable}

import akka.util.Timeout
import com.stratio.sparta.core.enumerators.SaveModeEnum
import com.stratio.sparta.core.models.ErrorValidations
import com.stratio.sparta.core.properties.JsoneyString
import com.stratio.sparta.plugin.TemporalSparkContext
import com.stratio.sparta.plugin.enumerations.MlPipelineSaveMode
import com.stratio.sparta.plugin.workflow.output.mlpipeline.MlPipelineOutputStep
import com.stratio.sparta.plugin.workflow.output.mlpipeline.validation.{GenericPipelineStepTest, ValidationErrorMessages}
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.sql.DataFrame
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Minutes, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, ShouldMatchers}

import scala.io.Source
import scala.util.Try
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class IndexToStringStepIT extends TemporalSparkContext with ShouldMatchers with BeforeAndAfterAll {

  self: FlatSpec =>

  override val timeLimit = Span(1, Minutes)

  override val timeout = Timeout(1 minutes)

  def stepName: String = "indextostring"

  def resourcesPath: String = "/mlpipeline/singlesteps/preprocessing/indextostring/"

  private def stepDefaultParamsPath = s"$resourcesPath$stepName-default-params-v0.json"

  private def stepEmptyParamsPath = s"$resourcesPath$stepName-empty-params-v0.json"

  private def stepWrongParamsPath = s"$resourcesPath$stepName-wrong-params-v0.json"

  private def stepWrongInputColumnPath = s"$resourcesPath$stepName-wrong-input-column-v0.json"

  lazy val trainingDf: DataFrame = {
    val df = sparkSession.createDataFrame(Seq(
      (0, "a"),
      (1, "b"),
      (2, "c"),
      (3, "a"),
      (4, "a"),
      (5, "c")
    )).toDF("id", "category")

    val indexer = new StringIndexer()
      .setInputCol("category")
      .setOutputCol("categoryIndex")
      .fit(df)
    indexer.transform(df)
  }

  trait ReadDescriptorResource {
    def getJsonDescriptor(filename: String): String = {
      Source.fromInputStream(getClass.getResourceAsStream(filename)).mkString
    }
  }

  trait WithExampleData {
    // Generate noisy input of the form Y = signum(x.dot(weights) + intercept + noise)
    def generateInputDf(): DataFrame = trainingDf
  }

  trait WithFilesystemProperties {
    var properties: Map[String, JSerializable] = Map(
      "output.mode" -> JsoneyString(MlPipelineSaveMode.FILESYSTEM.toString),
      "path" -> JsoneyString("/tmp/pipeline_tests")
    )
  }

  trait WithExecuteStep {
    def executeStep(training: DataFrame, properties: Map[String, JSerializable]) {
      // · Creating outputStep
      val mlPipelineOutput = new MlPipelineOutputStep("MlPipeline.out", sparkSession, properties)
      // · Executing step
      mlPipelineOutput.save(training, SaveModeEnum.Overwrite, Map.empty[String, String])
    }

    def executeStepAndUsePipeline(training: DataFrame, properties: Map[String, JSerializable]) {
      // · Creating outputStep
      val mlPipelineOutput = new MlPipelineOutputStep("MlPipeline.out", sparkSession, properties)
      // · Executing step
      mlPipelineOutput.save(training, SaveModeEnum.Overwrite, Map.empty[String, String])
      // · Use pipeline object
      val pipelineModel = mlPipelineOutput.pipeline.get.fit(training)
      val df = pipelineModel.transform(training)
      df.show()
    }
  }

  trait WithValidateStep {
    def validateMlPipelineStep(properties: Map[String, JSerializable]): ErrorValidations = {
      // · Creating outputStep
      val mlPipelineOutput = new MlPipelineOutputStep("MlPipeline.out", sparkSession, properties)
      // · Executing step
      val e = mlPipelineOutput.validate()
      e.messages.foreach(x => log.info(x.message))
      e
    }
  }

  /* -------------------------------------------------------------
   => Correct Pipeline construction with default params
  ------------------------------------------------------------- */

  s"$stepName default configuration values" should "provide a valid SparkMl pipeline" in
    new WithFilesystemProperties with WithExampleData with WithExecuteStep with WithValidateStep with ReadDescriptorResource {
      properties = properties.updated("pipeline", JsoneyString(getJsonDescriptor(stepDefaultParamsPath)))

      // Validation step mut be done correctly
      val validation = Try {
        validateMlPipelineStep(properties)
      }
      assert(validation.isSuccess)
      assert(validation.get.valid)

      val execution = Try {
        executeStepAndUsePipeline(generateInputDf(), properties)
      }
      assert(execution.isSuccess)
    }


  /* -------------------------------------------------------------
   => Correct Pipeline construction with empty params
  ------------------------------------------------------------- */

  s"$stepName with empty configuration values" should "provide a valid SparkMl pipeline using default values" in
    new ReadDescriptorResource with WithExampleData with WithExecuteStep with WithValidateStep
      with WithFilesystemProperties {

      properties = properties.updated("pipeline", JsoneyString(getJsonDescriptor(stepEmptyParamsPath)))

      // Validation step mut be done correctly
      val validation = Try {
        validateMlPipelineStep(properties)
      }
      assert(validation.isSuccess)

      val execution = Try {
        executeStepAndUsePipeline(generateInputDf(), properties)
      }
      assert(execution.isSuccess)

    }


  /* -------------------------------------------------------------
   => Wrong Pipeline construction with invalid params
  ------------------------------------------------------------- */

  s"$stepName with wrong configuration parmas" should "provide an invalid SparkMl pipeline" in
    new ReadDescriptorResource with WithExampleData with WithExecuteStep with WithValidateStep
      with WithFilesystemProperties {

      properties = properties.updated("pipeline", JsoneyString(getJsonDescriptor(stepWrongParamsPath)))

      // Validation step mut be done correctly
      val validation = Try {
        validateMlPipelineStep(properties)
      }
      assert(validation.isSuccess)

      val execution = Try {
        executeStepAndUsePipeline(generateInputDf(), properties)
      }
      assert(execution.isFailure)
      assert(execution.failed.get.getMessage.startsWith("Failed to execute user defined function"))
      log.info(execution.failed.get.toString)
    }


  /* -------------------------------------------------------------
   => Wrong Pipeline construction with input column not present in training DF
  ------------------------------------------------------------- */

  s"$stepName with invalid input column" should "provide an invalid SparkMl pipeline" in
    new ReadDescriptorResource with WithExampleData with WithExecuteStep with WithValidateStep
      with WithFilesystemProperties {

      properties = properties.updated("pipeline", JsoneyString(getJsonDescriptor(stepWrongInputColumnPath)))

      // Validation step mut be done correctly
      val validation = Try {
        validateMlPipelineStep(properties)
      }
      assert(validation.isSuccess)

      val execution = Try {
        executeStepAndUsePipeline(generateInputDf(), properties)
      }
      assert(execution.isFailure)
      assert(execution.failed.get.getMessage.startsWith(ValidationErrorMessages.schemaErrorInit))
      log.info(execution.failed.get.toString)
    }

}