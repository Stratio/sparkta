/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.cleanNulls

import com.stratio.sparta.plugin.TemporalSparkContext
import com.stratio.sparta.sdk.DistributedMonad.DistributedMonadImplicits
import com.stratio.sparta.sdk.workflow.enumerators.SaveModeEnum
import com.stratio.sparta.sdk.workflow.step.{OutputOptions, TransformationStepManagement}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class CleanNullsTransformStepStreamingIT extends TemporalSparkContext with Matchers with DistributedMonadImplicits {

  "A CleanNullsTransformStepStreaming" should "filter events that contains nulls in the column values" in {

    val schema = StructType(Seq(StructField("color", StringType), StructField("price", DoubleType)))
    val dataQueue1 = new mutable.Queue[RDD[Row]]()
    val data1 = Seq(
      new GenericRowWithSchema(Array("blue", 12.1), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array("red", null), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array("red", 12.2), schema).asInstanceOf[Row]
    )
    dataQueue1 += sc.parallelize(data1)
    val stream1 = ssc.queueStream(dataQueue1)
    val inputData = Map("step1" -> stream1)
    val outputOptions = OutputOptions(SaveModeEnum.Append, "stepName", "tableName", None, None)
    val result = new CleanNullsTransformStepStreaming(
      "dummy",
      outputOptions,
      TransformationStepManagement(),
      Option(ssc),
      sparkSession,
      Map()
    ).transform(inputData)
    val totalEvents = ssc.sparkContext.accumulator(0L, "Number of events received")

    result.ds.foreachRDD(rdd => {
      val streamingEvents = rdd.count()
      log.info(s" EVENTS COUNT : \t $streamingEvents")
      totalEvents += streamingEvents
      log.info(s" TOTAL EVENTS : \t $totalEvents")
      val streamingRegisters = rdd.collect()
      if (!rdd.isEmpty())
        streamingRegisters.foreach(row => assert(data1.contains(row)))
    })
    ssc.start()
    ssc.awaitTerminationOrTimeout(timeoutStreaming)
    ssc.stop()

    assert(totalEvents.value === 2)
  }

  "A CleanNullsTransformStepStreaming" should "no filter events when nulls are not present in values" in {

    val schema = StructType(Seq(StructField("color", StringType), StructField("price", DoubleType)))
    val dataQueue1 = new mutable.Queue[RDD[Row]]()
    val data1 = Seq(
      new GenericRowWithSchema(Array("blue", 12.1), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array("red", 1.1), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array("red", 12.2), schema).asInstanceOf[Row]
    )
    dataQueue1 += sc.parallelize(data1)
    val stream1 = ssc.queueStream(dataQueue1)
    val inputData = Map("step1" -> stream1)
    val outputOptions = OutputOptions(SaveModeEnum.Append, "stepName", "tableName", None, None)
    val result = new CleanNullsTransformStepStreaming(
      "dummy",
      outputOptions,
      TransformationStepManagement(),
      Option(ssc),
      sparkSession,
      Map()
    ).transform(inputData)
    val totalEvents = ssc.sparkContext.accumulator(0L, "Number of events received")

    result.ds.foreachRDD(rdd => {
      val streamingEvents = rdd.count()
      log.info(s" EVENTS COUNT : \t $streamingEvents")
      totalEvents += streamingEvents
      log.info(s" TOTAL EVENTS : \t $totalEvents")
      val streamingRegisters = rdd.collect()
      if (!rdd.isEmpty())
        streamingRegisters.foreach(row => assert(data1.contains(row)))
    })
    ssc.start()
    ssc.awaitTerminationOrTimeout(timeoutStreaming)
    ssc.stop()

    assert(totalEvents.value === 3)
  }

  "A CleanNullsTransformStepStreaming" should "filter events that contains nulls in all columns values" in {

    val schema = StructType(Seq(StructField("color", StringType), StructField("price", DoubleType)))
    val dataQueue1 = new mutable.Queue[RDD[Row]]()
    val data1 = Seq(
      new GenericRowWithSchema(Array("blue", null), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array(null, null), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array("red", null), schema).asInstanceOf[Row]
    )
    dataQueue1 += sc.parallelize(data1)
    val stream1 = ssc.queueStream(dataQueue1)
    val inputData = Map("step1" -> stream1)
    val outputOptions = OutputOptions(SaveModeEnum.Append, "stepName", "tableName", None, None)
    val result = new CleanNullsTransformStepStreaming(
      "dummy",
      outputOptions,
      TransformationStepManagement(),
      Option(ssc),
      sparkSession,
      Map("cleanMode" -> "all")
    ).transform(inputData)
    val totalEvents = ssc.sparkContext.accumulator(0L, "Number of events received")

    result.ds.foreachRDD(rdd => {
      val streamingEvents = rdd.count()
      log.info(s" EVENTS COUNT : \t $streamingEvents")
      totalEvents += streamingEvents
      log.info(s" TOTAL EVENTS : \t $totalEvents")
      val streamingRegisters = rdd.collect()
      if (!rdd.isEmpty())
        streamingRegisters.foreach(row => assert(data1.contains(row)))
    })
    ssc.start()
    ssc.awaitTerminationOrTimeout(timeoutStreaming)
    ssc.stop()

    assert(totalEvents.value === 2)
  }

  "A CleanNullsTransformStepStreaming" should "filter events that contains nulls in one column" in {

    val schema = StructType(Seq(StructField("color", StringType), StructField("price", DoubleType)))
    val dataQueue1 = new mutable.Queue[RDD[Row]]()
    val data1 = Seq(
      new GenericRowWithSchema(Array(null, 1.1), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array(null, 1.1), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array("red", null), schema).asInstanceOf[Row]
    )
    val columns ="""[
                   |{
                   |   "name":"color"
                   |}]
                   | """.stripMargin
    dataQueue1 += sc.parallelize(data1)
    val stream1 = ssc.queueStream(dataQueue1)
    val inputData = Map("step1" -> stream1)
    val outputOptions = OutputOptions(SaveModeEnum.Append, "stepName", "tableName", None, None)
    val result = new CleanNullsTransformStepStreaming(
      "dummy",
      outputOptions,
      TransformationStepManagement(),
      Option(ssc),
      sparkSession,
      Map("cleanMode" -> "any", "columns" -> columns)
    ).transform(inputData)
    val totalEvents = ssc.sparkContext.accumulator(0L, "Number of events received")

    result.ds.foreachRDD(rdd => {
      val streamingEvents = rdd.count()
      log.info(s" EVENTS COUNT : \t $streamingEvents")
      totalEvents += streamingEvents
      log.info(s" TOTAL EVENTS : \t $totalEvents")
      val streamingRegisters = rdd.collect()
      if (!rdd.isEmpty())
        streamingRegisters.foreach(row => assert(data1.contains(row)))
    })
    ssc.start()
    ssc.awaitTerminationOrTimeout(timeoutStreaming)
    ssc.stop()

    assert(totalEvents.value === 1)
  }

  "A CleanNullsTransformStepStreaming" should "filter events that contains nulls in two columns" in {

    val schema = StructType(Seq(StructField("color", StringType), StructField("price", DoubleType)))
    val dataQueue1 = new mutable.Queue[RDD[Row]]()
    val data1 = Seq(
      new GenericRowWithSchema(Array(null, 1.1), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array(null, null), schema).asInstanceOf[Row],
      new GenericRowWithSchema(Array("red", null), schema).asInstanceOf[Row]
    )
    val columns ="""[
                   |{
                   |   "name":"color"
                   |},
                   |{
                   |   "name":"price"
                   |}
                   |]
                   | """.stripMargin
    dataQueue1 += sc.parallelize(data1)
    val stream1 = ssc.queueStream(dataQueue1)
    val inputData = Map("step1" -> stream1)
    val outputOptions = OutputOptions(SaveModeEnum.Append, "stepName", "tableName", None, None)
    val result = new CleanNullsTransformStepStreaming(
      "dummy",
      outputOptions,
      TransformationStepManagement(),
      Option(ssc),
      sparkSession,
      Map("cleanMode" -> "all", "columns" -> columns)
    ).transform(inputData)
    val totalEvents = ssc.sparkContext.accumulator(0L, "Number of events received")

    result.ds.foreachRDD(rdd => {
      val streamingEvents = rdd.count()
      log.info(s" EVENTS COUNT : \t $streamingEvents")
      totalEvents += streamingEvents
      log.info(s" TOTAL EVENTS : \t $totalEvents")
      val streamingRegisters = rdd.collect()
      if (!rdd.isEmpty())
        streamingRegisters.foreach(row => assert(data1.contains(row)))
    })
    ssc.start()
    ssc.awaitTerminationOrTimeout(timeoutStreaming)
    ssc.stop()

    assert(totalEvents.value === 2)
  }
}