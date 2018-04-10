/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.datetime

import java.io.{Serializable => JSerializable}

import com.stratio.sparta.sdk.workflow.enumerators.SaveModeEnum
import com.stratio.sparta.sdk.workflow.step.{OutputOptions, TransformationStepManagement}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}

@RunWith(classOf[JUnitRunner])
class DateTimeTransformStepStreamingTest extends WordSpecLike
  with Matchers {

  "A DateTimeTransform" should {
    val outputOptions = OutputOptions(SaveModeEnum.Append, "stepName", "tableName", None, None)
    val schemaInput = StructType(Seq(StructField("ts", StringType)))

    //scalastyle:off
    "parse unix to string" in {

      val input = new GenericRowWithSchema(Array(1416330788000L), schemaInput)
      val fieldsDatetime =
        """[{
          |"inputField":"ts",
          |"formatFrom":"STANDARD",
          |"userFormat":"",
          |"standardFormat":"unixMillis",
          |"localeTime":"ENGLISH",
          |"granularityNumber":"",
          |"granularityTime":"millisecond",
          |"fieldsPreservationPolicy":"APPEND",
          |"outputFieldName":"ts1",
          |"outputFieldType":"long",
          |"outputFormatFrom": "DEFAULT"
          |}]
          |""".stripMargin

      val result =
        new DateTimeTransformStepStreaming(
          "testTransformation",
          outputOptions,
          TransformationStepManagement(),
          null,
          null,
          Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
          .parse(input)
      val schemaExpected = StructType(Seq(StructField("ts", StringType), StructField("ts1", StringType)))
      val expected = Seq(new GenericRowWithSchema(Array(1416330788000L, 1416330788000L), schemaExpected))
      assertResult(expected)(result)
    }

    "parse unix to string removing raw" in {

      val input = new GenericRowWithSchema(Array(1416330788000L), schemaInput)
      val fieldsDatetime =
        """[{
          |"inputField":"ts",
          |"formatFrom":"STANDARD",
          |"userFormat":"",
          |"standardFormat":"unixMillis",
          |"localeTime":"ENGLISH",
          |"granularityNumber":"",
          |"granularityTime":"millisecond",
          |"fieldsPreservationPolicy":"REPLACE",
          |"outputFieldName":"ts",
          |"outputFieldType":"string",
          |"outputFormatFrom": "DEFAULT"
          |}]
          |""".stripMargin

      val result =
        new DateTimeTransformStepStreaming(
          "testTransformation",
          outputOptions,
          TransformationStepManagement(),
          null,
          null,
          Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
          .parse(input)

      val schemaExpected = StructType(Seq(StructField("ts", StringType)))
      val expected = Seq(new GenericRowWithSchema(Array("1416330788000"), schemaExpected))
      assertResult(result)(expected)
    }

    "not parse anything if the field does not match" in {
      val schema = StructType(Seq(StructField("otherField", StringType)))
      val input = new GenericRowWithSchema(Array("1212"), schema)

      val fieldsDatetime =
        """[{
          |"inputField":"otherfield",
          |"formatFrom":"STANDARD",
          |"userFormat":"",
          |"standardFormat":"unixMillis",
          |"localeTime":"ENGLISH",
          |"granularityNumber":"",
          |"granularityTime":"millisecond",
          |"fieldsPreservationPolicy":"REPLACE",
          |"outputFieldName":"otherfield",
          |"outputFieldType":"string",
          |"outputFormatFrom": "DEFAULT"
          |}]
          |""".stripMargin

      an[Exception] should be thrownBy new DateTimeTransformStepStreaming(
        "testTransformation",
        outputOptions,
        TransformationStepManagement(),
        null,
        null,
        Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
        .parse(input)
    }

    "not parse anything and generate a new Date" in {

      val input = new GenericRowWithSchema(Array("1212"), schemaInput)

      val fieldsDatetime =
        """[{
          |"inputField":"anything",
          |"formatFrom":"AUTOGENERATED",
          |"userFormat":"",
          |"standardFormat":"unixMillis",
          |"localeTime":"ENGLISH",
          |"granularityNumber":"",
          |"granularityTime":"millisecond",
          |"fieldsPreservationPolicy":"APPEND",
          |"outputFieldName":"timestamp",
          |"outputFieldType":"string",
          |"outputFormatFrom": "DEFAULT"
          |}
          |]
          |""".stripMargin

      val result =
        new DateTimeTransformStepStreaming(
          "testTransformation",
          outputOptions,
          TransformationStepManagement(),
          null,
          null,
          Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
          .parse(input)

      assertResult(result.head.size)(2)
    }

    "append a value when formatFrom is autogenerated" in {
      val input = new GenericRowWithSchema(Array("1416330788"), schemaInput)

      val fieldsDatetime =
        """[{
          |"inputField":"ts",
          |"formatFrom":"",
          |"userFormat":"AUTOGENERATED",
          |"standardFormat":"unixMillis",
          |"granularityNumber":"",
          |"granularityTime":"millisecond",
          |"fieldsPreservationPolicy":"APPEND",
          |"outputFieldName":"ts1",
          |"outputFieldType":"string",
          |"outputFormatFrom": "DEFAULT"
          |}]
          |""".stripMargin

      val result =
        new DateTimeTransformStepStreaming(
          "testTransformation",
          outputOptions,
          TransformationStepManagement(),
          null,
          null,
          Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
          .parse(input)

      assertResult(result.head.size)(2)
    }

    "parse dateTime in hive format" in {
      val input = new GenericRowWithSchema(Array("2015-11-08 15:58:58"), schemaInput)

      val fieldsDatetime =
        """[{
          |"inputField":"ts",
          |"formatFrom":"STANDARD",
          |"userFormat":"",
          |"standardFormat":"hive",
          |"localeTime":"ENGLISH",
          |"granularityNumber":"",
          |"granularityTime":"millisecond",
          |"fieldsPreservationPolicy":"APPEND",
          |"outputFieldName":"ts1",
          |"outputFieldType":"string",
          |"outputFormatFrom": "DEFAULT"
          |}]
          |""".stripMargin

      val result =
        new DateTimeTransformStepStreaming(
          "testTransformation",
          outputOptions,
          TransformationStepManagement(),
          null,
          null,
          Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
          .parse(input)

      val schemaExpected = StructType(Seq(StructField("ts", StringType), StructField("ts1", StringType)))
      val expected = Seq(new GenericRowWithSchema(Array("2015-11-08 15:58:58", "1446998338000"), schemaExpected))
      assertResult(expected)(result)
    }

    "parse dateTime in hive format and then back to a new format" in {
      val input = new GenericRowWithSchema(Array("2015-11-08 15:58:58"), schemaInput)

      val fieldsDatetime =
        """[{
          |"inputField":"ts",
          |"formatFrom":"STANDARD",
          |"userFormat":"",
          |"standardFormat":"hive",
          |"localeTime":"ENGLISH",
          |"granularityNumber":"",
          |"granularityTime":"millisecond",
          |"fieldsPreservationPolicy":"APPEND",
          |"outputFieldName":"ts1",
          |"outputFieldType":"string",
          |"outputFormatFrom": "USER",
          |"outputUserFormat": "dd-MMM-YYYY"
          |}]
          |""".stripMargin

      val result =
        new DateTimeTransformStepStreaming(
          "testTransformation",
          outputOptions,
          TransformationStepManagement(),
          null,
          null,
          Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
          .parse(input)

      val schemaExpected = StructType(Seq(StructField("ts", StringType), StructField("ts1", StringType)))
      val expected = Seq(new GenericRowWithSchema(Array("2015-11-08 15:58:58", "08-Nov-2015"), schemaExpected))
      assertResult(expected)(result)
    }

    "throw an error if a policy sets USER as outputFormatFrom and the FieldType is not String" in {
      val input = new GenericRowWithSchema(Array("2015-11-08 15:58:58"), schemaInput)
      val fieldsDatetime =
        """[{
          |"inputField":"ts",
          |"formatFrom":"AUTOGENERATED",
          |"userFormat":"",
          |"standardFormat":"hive",
          |"localeTime":"ENGLISH",
          |"granularityNumber":"",
          |"granularityTime":"millisecond(s)",
          |"fieldsPreservationPolicy":"REPLACE",
          |"outputFieldName":"ts",
          |"outputFieldType":"timestamp",
          |"outputFormatFrom": "user",
          |"outputUserFormat": "dd-MMM-YYYY"
          |}]
          |""".stripMargin

      an[Exception] should be thrownBy new DateTimeTransformStepStreaming(
        "testTransformation",
        outputOptions,
        TransformationStepManagement(),
        null,
        null,
        Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
        .parse(input)
    }

    "throw an error if a policy sets STANDARD as outputFormatFrom and the FieldType is not String" in {
      val input = new GenericRowWithSchema(Array("2015-11-08 15:58:58"), schemaInput)
      val fieldsDatetime =
        """[{
          |"inputField":"ts",
          |"formatFrom":"AUTOGENERATED",
          |"userFormat":"",
          |"standardFormat":"hive",
          |"localeTime":"ENGLISH",
          |"granularityNumber":"",
          |"granularityTime":"millisecond(s)",
          |"fieldsPreservationPolicy":"REPLACE",
          |"outputFieldName":"ts",
          |"outputFieldType":"timestamp",
          |"outputFormatFrom": "STANDARD",
          |"outputStandardFormat": "hive"
          |}]
          |""".stripMargin

      an[Exception] should be thrownBy new DateTimeTransformStepStreaming(
        "testTransformation",
        outputOptions,
        TransformationStepManagement(),
        null,
        null,
        Map("fieldsDatetime" -> fieldsDatetime.asInstanceOf[JSerializable]))
        .parse(input)
    }
  }
}