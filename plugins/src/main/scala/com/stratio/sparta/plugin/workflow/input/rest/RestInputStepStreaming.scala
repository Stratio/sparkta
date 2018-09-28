/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.input.rest

import java.io.{Serializable => JSerializable}

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.core.DistributedMonad
import com.stratio.sparta.core.DistributedMonad.Implicits._
import com.stratio.sparta.core.models.OutputOptions
import com.stratio.sparta.plugin.common.rest.RestUtils.WithPreprocessing
import com.stratio.sparta.plugin.common.rest.{RestGraph, SparkExecutorRestUtils}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.test.TestDStream

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RestInputStepStreaming(name: String,
                             outputOptions: OutputOptions,
                             ssc: Option[StreamingContext],
                             xDSession: XDSession,
                             properties: Map[String, JSerializable]
                            )
  extends RestInputStep[DStream](name, outputOptions, ssc, xDSession, properties) with SLF4JLogging {

  override def init(): DistributedMonad[DStream] = {

    val triggerRDD = xDSession.sparkContext.parallelize(Array(triggerRow))

    val triggerDStream = new TestDStream(ssc.get,triggerRDD)

    val defaultRDStream = triggerDStream.transform { inputRDD =>
      inputRDD.mapPartitions { rowsIterator =>

        implicit val restUtils: SparkExecutorRestUtils = SparkExecutorRestUtils.getOrCreate(restConfig.akkaHttpProperties)

        import restUtils.Implicits._

        // We wait for the future containing our rows & results to complete ...
        val seqSolRow: Iterator[(String, Row)] =
          Await.result(RestGraph(restConfig, restUtils).createInputGraph(rowsIterator,
            Map.empty[String, WithPreprocessing], Map.empty[String, WithPreprocessing], None).run(), Duration.Inf).toIterator

        /** ... we create the output RDD with the rows according to the chosen preservation policy preserving
          * the old row because mapPartitions has as output only a RDD[T]
          * so we create a RDD[(Row, Row)] ~> RDD[(ProcessedRow, InputRow)]
          * */
        seqSolRow.map { case (response, _) =>
          new GenericRowWithSchema(Array(response), responseStringSchema): Row
        }
      }
    }
    defaultRDStream.registerAsTable(xDSession, responseStringSchema, name)
    defaultRDStream
  }
}
