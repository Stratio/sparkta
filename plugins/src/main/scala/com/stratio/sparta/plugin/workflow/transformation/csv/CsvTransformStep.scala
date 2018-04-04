/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.csv

import java.io.{Serializable => JSerializable}
import java.util.regex.Pattern

import com.stratio.sparta.plugin.enumerations.SchemaInputMode._
import com.stratio.sparta.plugin.enumerations.{FieldsPreservationPolicy, SchemaInputMode}
import com.stratio.sparta.plugin.helper.SchemaHelper._
import com.stratio.sparta.sdk.DistributedMonad
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.step._
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.streaming.StreamingContext

import scala.util.{Failure, Success, Try}


abstract class CsvTransformStep[Underlying[Row]](
                                                  name: String,
                                                  outputOptions: OutputOptions,
                                                  transformationStepsManagement: TransformationStepManagement,
                                                  ssc: Option[StreamingContext],
                                                  xDSession: XDSession,
                                                  properties: Map[String, JSerializable]
                                                )(implicit dsMonadEvidence: Underlying[Row] => DistributedMonad[Underlying])
  extends TransformStep[Underlying](name, outputOptions, transformationStepsManagement, ssc, xDSession, properties) {

  lazy val schemaInputMode = SchemaInputMode.withName(properties.getString("schema.inputMode", "HEADER").toUpperCase)

  lazy val fieldsModel = properties.getPropertiesFields("schema.fields")

  lazy val header = properties.getString("schema.header", None)

  lazy val sparkSchema = properties.getString("schema.sparkSchema", None)

  lazy val fieldsSeparator = properties.getString("delimiter", ",")

  lazy val splitLimit = properties.getInt("splitLimit", -1)

  lazy val delimiterType = DelimiterType.withName(properties.getString("delimiterType", "CHARACTER").toUpperCase)

  lazy val inputField = Try(properties.getString("inputField"))
    .getOrElse(throw new Exception("The inputField is mandatory"))

  lazy val preservationPolicy: FieldsPreservationPolicy.Value = FieldsPreservationPolicy.withName(
    properties.getString("fieldsPreservationPolicy", "REPLACE").toUpperCase)

  lazy val providedSchema: Seq[StructField] = {
    (schemaInputMode, header, sparkSchema, fieldsModel) match {
      case (HEADER, Some(headerStr), _, _) =>
        headerStr.split(Pattern.quote(fieldsSeparator))
          .map(fieldName => StructField(fieldName, StringType, nullable = true)).toSeq
      case (SPARKFORMAT, None, Some(schema), _) =>
        getSparkSchemaFromString(schema).map(_.fields.toSeq).getOrElse(Seq.empty)
      case (FIELDS, _, _, inputFields) if inputFields.fields.nonEmpty =>
        inputFields.fields.map { fieldModel =>
          val outputType = fieldModel.`type`.notBlank.getOrElse("string")
          StructField(
            name = fieldModel.name,
            dataType = SparkTypes.get(outputType) match {
              case Some(sparkType) => sparkType
              case None => schemaFromString(outputType)
            },
            nullable = fieldModel.nullable.getOrElse(true)
          )
        }
      case _ => throw new Exception("Incorrect schema arguments")

    }
  }

  assert(inputField.nonEmpty)

  def transformationFunction(
                              inputSchema: String,
                              inputStream: DistributedMonad[Underlying]
                            ): DistributedMonad[Underlying] =
    inputStream.flatMap(data => parse(data))


  override def transform(inputData: Map[String, DistributedMonad[Underlying]]): DistributedMonad[Underlying] =
    applyHeadTransform(inputData)(transformationFunction)

  //scalastyle:off
  def parse(row: Row): Seq[Row] =
    returnSeqDataFromRow {
      val inputSchema = row.schema
      val outputSchema = getNewOutputSchema(inputSchema, preservationPolicy, providedSchema, inputField)
      val inputValue = Option(row.get(inputSchema.fieldIndex(inputField)))
      val newValues =
        inputValue match {
          case Some(value) =>
            if (value.toString.nonEmpty) {
              val valuesSplit = {
                val valueStr = value match {
                  case valueCast: Array[Byte] => new Predef.String(valueCast)
                  case valueCast: String => valueCast
                  case _ => value.toString
                }
                delimiterType match {
                  case DelimiterType.REGEX =>
                    valueStr.split(Pattern.compile(fieldsSeparator).toString, splitLimit)
                  case DelimiterType.CHARACTER =>
                    valueStr.split(Pattern.quote(fieldsSeparator), splitLimit)
                  case _ =>
                    valueStr.split(fieldsSeparator, splitLimit)
                }
              }

              if (valuesSplit.length == providedSchema.length) {
                val valuesParsed = providedSchema.map(_.name).zip(valuesSplit).toMap

                outputSchema.map { outputField =>
                  Try {
                    valuesParsed.get(outputField.name) match {
                      case Some(valueParsed) =>
                        if (valueParsed == "null") null
                        else castingToOutputSchema(outputField, valueParsed)
                      case None =>
                        row.get(inputSchema.fieldIndex(outputField.name))
                    }
                  } match {
                    case Success(newValue) =>
                      newValue
                    case Failure(e) =>
                      returnWhenFieldError(
                        new Exception(s"Impossible to parse outputField: $outputField " +
                          s"from extracted values: ${valuesParsed.keys.mkString(",")}", e))
                  }
                }
              } else throw new Exception(s"The number of values splitted does not match the number of " +
                s"fields defined in the schema")
            } else throw new Exception(s"The input value is empty")
          case None =>
            throw new Exception(s"The input value is null")
        }
      new GenericRowWithSchema(newValues.toArray, outputSchema)
    }
}
