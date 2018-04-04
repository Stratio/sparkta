/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.filter

import java.io.{Serializable => JSerializable}

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.sdk.DistributedMonad
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.step.{ErrorValidations, OutputOptions, TransformStep, TransformationStepManagement}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.streaming.StreamingContext

import scala.util.{Failure, Success, Try}

abstract class FilterTransformStep[Underlying[Row]](
                                                     name: String,
                                                     outputOptions: OutputOptions,
                                                     transformationStepsManagement: TransformationStepManagement,
                                                     ssc: Option[StreamingContext],
                                                     xDSession: XDSession,
                                                     properties: Map[String, JSerializable]
                                                   )(implicit dsMonadEvidence: Underlying[Row] => DistributedMonad[Underlying])
  extends TransformStep[Underlying](name, outputOptions, transformationStepsManagement, ssc, xDSession, properties) {

  lazy val filterExpression: Option[String] = properties.getString("filterExp", None).notBlank

  override def validate(options: Map[String, String] = Map.empty[String, String]): ErrorValidations = {
    var validation = ErrorValidations(valid = true, messages = Seq.empty)

    if (filterExpression.isEmpty)
      validation = ErrorValidations(
        valid = false,
        messages = validation.messages :+ s"$name: it's mandatory one filter expression, such as colA > 10"
      )

    validation
  }

  def applyFilter(rdd: RDD[Row], expression: String): RDD[Row] = {
    Try {
      if (rdd.isEmpty()) rdd
      else {
        val schema = rdd.first().schema
        val df = xDSession.createDataFrame(rdd, schema)

        df.filter(expression).rdd
      }
    } match {
      case Success(sqlResult) => sqlResult
      case Failure(e) => rdd.map(_ => Row.fromSeq(throw e))
    }
  }
}

