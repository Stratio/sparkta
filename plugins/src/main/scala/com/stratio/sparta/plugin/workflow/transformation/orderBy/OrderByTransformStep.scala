/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.orderBy

import java.io.{Serializable => JSerializable}

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.plugin.helper.SchemaHelper.{createOrReplaceTemporalViewDf, getSchemaFromSessionOrModelOrRdd, parserInputSchema}
import com.stratio.sparta.sdk.DistributedMonad
import com.stratio.sparta.sdk.helpers.SdkSchemaHelper
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.step.{ErrorValidations, OutputOptions, TransformStep, TransformationStepManagement}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.sql.types.StructType
import org.apache.spark.streaming.StreamingContext

import scala.util.{Failure, Success, Try}


abstract class OrderByTransformStep[Underlying[Row]](
                                                      name: String,
                                                      outputOptions: OutputOptions,
                                                      transformationStepsManagement: TransformationStepManagement,
                                                      ssc: Option[StreamingContext],
                                                      xDSession: XDSession,
                                                      properties: Map[String, JSerializable])
                                                    (implicit dsMonadEvidence: Underlying[Row] => DistributedMonad[Underlying])
  extends TransformStep[Underlying](name, outputOptions, transformationStepsManagement, ssc, xDSession, properties)
    with SLF4JLogging {

  lazy val orderExpression: Option[String] = properties.getString("orderExp", None).notBlank

  def requireValidateSql(): Unit = {
    val sql = s"select dummyCol from dummyTable order by ${orderExpression.getOrElse("dummyCol")}"
    require(sql.nonEmpty, "The input query can not be empty")
    require(validateSql, "The input query is invalid")
  }

  def validateSql: Boolean =
    Try {
      val sql = s"select dummyCol from dummyTable order by ${orderExpression.getOrElse("dummyCol")}"
      xDSession.sessionState.sqlParser.parsePlan(sql)
    } match {
      case Success(_) =>
        true
      case Failure(e) =>
        log.warn(s"$name invalid sql. ${e.getLocalizedMessage}")
        false
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

    if (orderExpression.isEmpty)
      validation = ErrorValidations(
        valid = false,
        messages = validation.messages :+ s"$name: it's mandatory one order expression, such as colA, colB"
      )

    if (orderExpression.nonEmpty && !validateSql)
      validation = ErrorValidations(
        valid = false,
        messages = validation.messages :+ s"$name: the order by expression is invalid"
      )

    validation
  }

  def applyOrderBy(rdd: RDD[Row], expression: String, inputStep: String): (RDD[Row], Option[StructType]) = {
    Try {
      val schema = getSchemaFromSessionOrModelOrRdd(xDSession, inputStep, inputsModel, rdd)
      createOrReplaceTemporalViewDf(xDSession, rdd, inputStep, schema) match {
        case Some(_) =>
          val newDataFrame = xDSession.sql(s"select * from $inputStep order by $expression")
          (newDataFrame.rdd, Option(newDataFrame.schema))
        case None =>
          (rdd.filter(_ => false), None)
      }
    } match {
      case Success(sqlResult) => sqlResult
      case Failure(e) => (rdd.map(_ => Row.fromSeq(throw e)), None)
    }
  }

}