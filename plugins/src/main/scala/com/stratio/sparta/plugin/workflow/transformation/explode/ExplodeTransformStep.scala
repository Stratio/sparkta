/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.explode

import java.io.{Serializable => JSerializable}
import java.util

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import akka.event.slf4j.SLF4JLogging
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.streaming.StreamingContext
import com.stratio.sparta.plugin.enumerations.SchemaInputMode.{FIELDS, SPARKFORMAT}
import com.stratio.sparta.plugin.enumerations.{FieldsPreservationPolicy, SchemaInputMode}
import com.stratio.sparta.plugin.helper.SchemaHelper.{getNewOutputSchema, getSparkSchemaFromString}
import com.stratio.sparta.sdk.DistributedMonad
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.step._
import com.stratio.sparta.sdk.utils.CastingUtils._

abstract class ExplodeTransformStep[Underlying[Row]](
                                                      name: String,
                                                      outputOptions: OutputOptions,
                                                      transformationStepsManagement: TransformationStepManagement,
                                                      ssc: Option[StreamingContext],
                                                      xDSession: XDSession,
                                                      properties: Map[String, JSerializable]
                                                    )(implicit dsMonadEvidence: Underlying[Row] => DistributedMonad[Underlying])
  extends TransformStep[Underlying](name, outputOptions, transformationStepsManagement, ssc, xDSession, properties)
    with SLF4JLogging {

  lazy val inputField: String = Try(properties.getString("inputField"))
    .getOrElse(throw new IllegalArgumentException("The inputField is mandatory"))

  lazy val preservationPolicy: FieldsPreservationPolicy.Value = FieldsPreservationPolicy.withName(
    properties.getString("fieldsPreservationPolicy", "REPLACE").toUpperCase)

  lazy val providedSchema: Option[Seq[StructField]] = {
    if (properties.getBoolean("schema.fromRow", default = true)) None
    else {
      val fieldsModel = properties.getPropertiesFields("schema.fields")
      val sparkSchema = properties.getString("schema.sparkSchema", None)
      val schemaInputMode = SchemaInputMode.withName(properties.getString("schema.inputMode", "FIELDS").toUpperCase)
      (schemaInputMode, sparkSchema, fieldsModel) match {
        case (SPARKFORMAT, Some(schema), _) =>
          getSparkSchemaFromString(schema).map(_.fields.toSeq).toOption
        case (FIELDS, _, inputFields) if inputFields.fields.nonEmpty =>
          Option(inputFields.fields.map { fieldModel =>
            val outputType = fieldModel.`type`.notBlank.getOrElse("string")
            StructField(
              name = fieldModel.name,
              dataType = SparkTypes.get(outputType) match {
                case Some(sparkType) => sparkType
                case None => schemaFromString(outputType)
              },
              nullable = fieldModel.nullable.getOrElse(true)
            )
          })
        case _ => throw new Exception("Incorrect schema arguments")
      }
    }
  }

  override def transform(inputData: Map[String, DistributedMonad[Underlying]]): DistributedMonad[Underlying] =
    applyHeadTransform(inputData) { (_, inputStream) =>
      inputStream.flatMap(data => parse(data))
    }

  //scalastyle:off
  def parse(row: Row): Seq[Row] =
    returnSeqDataFromRows {
      val inputSchema = row.schema
      Option(row.get(inputSchema.fieldIndex(inputField))) match {
        case Some(value) =>
          val (rowFieldValues, rowFieldSchema) = value match {
            case valueCast: GenericRowWithSchema =>
              (Seq(valueCast), valueCast.schema)
            case _ =>
              Try {
                val valueInstance = checkArrayStructType(value).asInstanceOf[Seq[GenericRowWithSchema]]
                if (valueInstance.nonEmpty) (valueInstance, valueInstance.head.schema)
                else (valueInstance, StructType(Nil))
              } match {
                case Success(x) => x
                case Failure(e) => throw new Exception(s"The input value has incorrect type Seq(Map()) or Seq(Row). Value:${value.toString}", e)
              }
          }

          val outputSchema = getNewOutputSchema(inputSchema, preservationPolicy,
            providedSchema.getOrElse(rowFieldSchema.fields.toSeq), inputField)

          rowFieldValues.map { valuesMap =>
            val newValues = outputSchema.map { outputField =>
              Try {
                Try(valuesMap.get(valuesMap.fieldIndex(outputField.name))) match {
                  case Success(parsedValue) if parsedValue != null => castingToOutputSchema(outputField, parsedValue)
                  case _ => row.get(inputSchema.fieldIndex(outputField.name))
                }
              } match {
                case Success(correctValue) => correctValue
                case Failure(e) => returnWhenFieldError(new Exception(s"Impossible to parse outputField: $outputField in the schema", e))
              }
            }
            new GenericRowWithSchema(newValues.toArray, outputSchema)
          }
        case None =>
          throw new Exception(s"The input value is null")
      }
    }
}