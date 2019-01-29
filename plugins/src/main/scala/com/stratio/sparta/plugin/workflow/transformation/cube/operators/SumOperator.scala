/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.cube.operators

import com.stratio.sparta.core.enumerators.WhenFieldError.WhenFieldError
import com.stratio.sparta.core.enumerators.WhenRowError.WhenRowError
import com.stratio.sparta.core.helpers.CastingHelper
import com.stratio.sparta.plugin.workflow.transformation.cube.sdk.{Associative, Operator}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import scala.util.Try

class SumOperator(
                   name: String,
                   val whenRowErrorDo: WhenRowError,
                   val whenFieldErrorDo: WhenFieldError,
                   inputField: Option[String] = None
                 ) extends Operator(name, whenRowErrorDo, whenFieldErrorDo, inputField) with Associative {

  assert(inputField.isDefined)

  override val defaultOutputType: DataType = DoubleType

  override def processMap(inputRow: Row): Option[Any] = processMapFromInputField(inputRow)

  override def processReduce(values: Iterable[Option[Any]]): Option[Any] =
    returnFromTryWithNullCheck("Error in SumOperator when reducing values") {
      Try {
        sumCheckingType(values.flatten)
      }
    }

  def associativity(values: Iterable[(String, Option[Any])]): Option[Any] =
    returnFromTryWithNullCheck("Error in SumOperator when associating values") {
      Try {
        sumCheckingType(extractValues(values, None))
      }
    }

  def sumCheckingType(values: Iterable[Any]): Any =
    values.head match {
      case _: Double =>
        values.map(CastingHelper.castingToSchemaType(DoubleType, _).asInstanceOf[Double]).sum
      case _: Float =>
        values.map(CastingHelper.castingToSchemaType(FloatType, _).asInstanceOf[Float]).sum
      case _: Int =>
        values.map(CastingHelper.castingToSchemaType(IntegerType, _).asInstanceOf[Int]).sum
      case _: Short =>
        values.map(CastingHelper.castingToSchemaType(ShortType, _).asInstanceOf[Short]).sum
      case _: Long =>
        values.map(CastingHelper.castingToSchemaType(LongType, _).asInstanceOf[Long]).sum
      case _: java.sql.Timestamp =>
        values.map(CastingHelper.castingToSchemaType(LongType, _).asInstanceOf[Long]).sum
      case _: String =>
        values.map(CastingHelper.castingToSchemaType(DoubleType, _).asInstanceOf[Double]).sum
      case _ =>
        throw new Exception(s"Unsupported type in SumOperator $name")
    }
}
