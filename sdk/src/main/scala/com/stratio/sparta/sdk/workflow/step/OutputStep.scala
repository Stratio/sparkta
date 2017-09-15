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

package com.stratio.sparta.sdk.workflow.step

import java.io.{Serializable => JSerializable}

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.sdk.properties.Parameterizable
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.enumerators.SaveModeEnum
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.sql.types.{StructType, _}
import org.apache.spark.sql.{DataFrame, DataFrameWriter, Row, SaveMode}
import org.apache.spark.streaming.dstream.DStream

import scala.util.{Failure, Success, Try}

abstract class OutputStep(
                           val name: String,
                           @transient private[sparta] val xDSession: XDSession,
                           properties: Map[String, JSerializable]
                         ) extends Parameterizable(properties) with GraphStep with SLF4JLogging {

  val PrimaryKey = "primaryKey"
  val TableNameKey = "tableName"
  val PartitionByKey = "partitionBy"

  /**
   * Generic write function that receive the stream data and pass to dataFrame, after this call the save function.
   *
   * @param inputData Input stream data to save
   * @param outputOptions Options to save
   */
  def writeTransform(inputData: DStream[Row], outputOptions: OutputOptions): Unit = {
    inputData.foreachRDD(rdd =>
      if (!rdd.isEmpty()) {
        val schema = rdd.first().schema
        val dataFrame = xDSession.createDataFrame(rdd, schema)
        val saveOptions = Map(TableNameKey -> outputOptions.tableName) ++
          outputOptions.partitionBy.notBlank.fold(Map.empty[String, String]) { partition =>
            Map(PartitionByKey -> partition)
          } ++
          outputOptions.primaryKey.notBlank.fold(Map.empty[String, String]) { key =>
            Map(PrimaryKey -> key)
          }

        Try {
          save(dataFrame, outputOptions.saveMode, saveOptions)
        } match {
          case Success(_) =>
            log.debug(s"Data saved in ${outputOptions.tableName}")
          case Failure(e) =>
            log.error(s"Error saving data. Table: ${outputOptions.tableName}\n\t" +
              s"Schema: ${dataFrame.schema}\n\tHead element: ${dataFrame.head}\n\t" +
              s"Error message: ${e.getMessage}", e)
        }
      }
    )
  }

  /**
   * Save function that implements the plugins.
   *
   * @param dataFrame The dataFrame to save
   * @param saveMode The sparta save mode selected
   * @param options Options to save the data (partitionBy, primaryKey ... )
   */
  def save(dataFrame: DataFrame, saveMode: SaveModeEnum.Value, options: Map[String, String]): Unit

  /** PRIVATE METHODS **/

  private[sparta] def supportedSaveModes: Seq[SaveModeEnum.Value] = SaveModeEnum.allSaveModes

  private[sparta] def validateSaveMode(saveMode: SaveModeEnum.Value): Unit = {
    if (!supportedSaveModes.contains(saveMode))
      log.warn(s"Save mode $saveMode selected not supported by the output $name." +
        s" Using the default mode ${SaveModeEnum.Append}"
      )
  }

  private[sparta] def getSparkSaveMode(saveModeEnum: SaveModeEnum.Value): SaveMode =
    saveModeEnum match {
      case SaveModeEnum.Append => SaveMode.Append
      case SaveModeEnum.ErrorIfExists => SaveMode.ErrorIfExists
      case SaveModeEnum.Overwrite => SaveMode.Overwrite
      case SaveModeEnum.Ignore => SaveMode.Ignore
      case SaveModeEnum.Upsert => SaveMode.Append
      case _ =>
        log.warn(s"Save Mode $saveModeEnum not supported, using default save mode ${SaveModeEnum.Append}")
        SaveMode.Append
    }

  private[sparta] def getPrimaryKeyOptions(options: Map[String, String]): Option[String] =
    options.get(PrimaryKey).notBlank

  private[sparta] def getTableNameFromOptions(options: Map[String, String]): String =
    options.getOrElse(TableNameKey, {
      log.error("Table name not defined")
      throw new NoSuchElementException("tableName not found in options")
    })

  private[sparta] def applyPartitionBy(options: Map[String, String],
                                       dataFrame: DataFrameWriter[Row],
                                       schemaFields: Array[StructField]): DataFrameWriter[Row] = {

    options.get(PartitionByKey).notBlank.fold(dataFrame)(partitions => {
      val fieldsInDataFrame = schemaFields.map(field => field.name)
      val partitionFields = partitions.split(",")
      if (partitionFields.forall(field => fieldsInDataFrame.contains(field)))
        dataFrame.partitionBy(partitionFields: _*)
      else {
        log.warn(s"Impossible to execute partition by fields: $partitionFields because the dataFrame not contain all" +
          s" fields. The dataFrame only contains: ${fieldsInDataFrame.mkString(",")}")
        dataFrame
      }
    })
  }
}

object OutputStep {

  val StepType = "output"
}