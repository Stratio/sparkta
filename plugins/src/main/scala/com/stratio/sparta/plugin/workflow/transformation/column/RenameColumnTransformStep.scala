/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.sparta.plugin.workflow.transformation.column

import java.io.{Serializable => JSerializable}

import scala.util.{Failure, Success, Try}
import akka.event.slf4j.SLF4JLogging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.sql.types.StructType
import org.apache.spark.streaming.StreamingContext
import org.json4s.jackson.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import com.stratio.sparta.plugin.helper.SchemaHelper.{createOrReplaceTemporalViewDf, getSchemaFromSessionOrModelOrRdd, parserInputSchema}
import com.stratio.sparta.plugin.helper.SqlHelper
import com.stratio.sparta.plugin.models.PropertyColumn
import com.stratio.sparta.sdk.DistributedMonad
import com.stratio.sparta.sdk.helpers.SdkSchemaHelper
import com.stratio.sparta.sdk.models.DiscardCondition
import com.stratio.sparta.sdk.properties.JsoneyStringSerializer
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.step.{ErrorValidations, OutputOptions, TransformStep, TransformationStepManagement}

//scalastyle:off
abstract class RenameColumnTransformStep[Underlying[Row]](
                                                           name: String,
                                                           outputOptions: OutputOptions,
                                                           transformationStepsManagement: TransformationStepManagement,
                                                           ssc: Option[StreamingContext],
                                                           xDSession: XDSession,
                                                           properties: Map[String, JSerializable]
                                                         )(implicit dsMonadEvidence: Underlying[Row] => DistributedMonad[Underlying])
  extends TransformStep[Underlying](name, outputOptions, transformationStepsManagement, ssc, xDSession, properties)
    with SLF4JLogging {

  lazy val columnsToRename: Seq[PropertyColumn] = {
      implicit val json4sJacksonFormats: Formats =
        DefaultFormats +
          new JsoneyStringSerializer()
      val cols = s"${properties.getString("columns", None).notBlank.fold("[]") { values => values.toString }}"
      read[Seq[PropertyColumn]](cols)
    }

  override def validate(options: Map[String, String] = Map.empty[String, String]): ErrorValidations = {
    var validation = ErrorValidations(valid = true, messages = Seq.empty)

    if (!SdkSchemaHelper.isCorrectTableName(name))
      validation = ErrorValidations(
        valid = false,
        messages = validation.messages :+ s"$name: the step name $name is not valid")

    //If contains schemas, validate if it can be parsed
    if (inputsModel.inputSchemas.nonEmpty) {
      inputsModel.inputSchemas.foreach { input =>
        if (parserInputSchema(input.schema).isFailure)
          validation = ErrorValidations(
            valid = false,
            messages = validation.messages :+ s"$name: the input schema from step ${input.stepName} is not valid")
      }

      inputsModel.inputSchemas.filterNot(is => SdkSchemaHelper.isCorrectTableName(is.stepName)).foreach { is =>
        validation = ErrorValidations(
          valid = false,
          messages = validation.messages :+ s"$name: the input table name ${is.stepName} is not valid")
      }
    }

    if (columnsToRename.isEmpty)
      validation = ErrorValidations(
        valid = false,
        messages = validation.messages :+ s"$name: it's mandatory at least one column to rename"
      )

    columnsToRename.filter(c => c.name.trim.isEmpty || c.alias.get.trim.isEmpty).foreach(c =>
      validation = ErrorValidations(
        valid = false,
        messages = validation.messages :+ s"$name: column name $c has an invalid format"
      )
    )

    validation
  }

  def applyRename(rdd: RDD[Row], inputStep: String): (RDD[Row], Option[StructType], Option[StructType]) = {
    Try {
      val inputSchema = getSchemaFromSessionOrModelOrRdd(xDSession, inputStep, inputsModel, rdd)
      createOrReplaceTemporalViewDf(xDSession, rdd, inputStep, inputSchema) match {
        case Some(df) =>
          val newDataFrame = columnsToRename.foldLeft(df) {
            (df, toRename) => df.withColumnRenamed(toRename.name, toRename.alias.get)
          }
          (newDataFrame.rdd, Option(newDataFrame.schema), inputSchema)
        case None =>
          (rdd.filter(_ => false), None, inputSchema)
      }
    } match {
      case Success(sqlResult) => sqlResult
      case Failure(e) => (SqlHelper.failWithException(rdd, e), None, None)
    }
  }
}