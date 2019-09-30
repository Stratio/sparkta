/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

package com.stratio.sparta.core.models.qualityrule

import com.stratio.sparta.core.models.SpartaQualityRulePredicate
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import scala.util.{Failure, Success, Try}

trait Operation {

  val spartaPredicate : SpartaQualityRulePredicate
  val schema : StructType

  def operation[T]: Row => Boolean

  def nullPointerExceptionHandler[T](function1: => Function1[Row, Boolean], ifNull: Boolean = false): Row => Boolean =
    (row: Row) => Try{ function1(row) }.recover{case _: NullPointerException => ifNull}.get

  lazy val field: String = spartaPredicate.field

  lazy val fieldType: DataType = schema.apply(field).dataType

}
