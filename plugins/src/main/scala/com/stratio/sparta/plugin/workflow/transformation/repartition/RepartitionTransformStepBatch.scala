/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.repartition

import java.io.{Serializable => JSerializable}

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.sdk.DistributedMonad
import com.stratio.sparta.sdk.DistributedMonad.Implicits._
import com.stratio.sparta.sdk.workflow.step.{OutputOptions, TransformationStepManagement}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.streaming.StreamingContext

class RepartitionTransformStepBatch(
                                     name: String,
                                     outputOptions: OutputOptions,
                                     transformationStepsManagement: TransformationStepManagement,
                                     ssc: Option[StreamingContext],
                                     xDSession: XDSession,
                                     properties: Map[String, JSerializable]
                                   )
  extends RepartitionTransformStep[RDD](name, outputOptions, transformationStepsManagement, ssc, xDSession, properties)
    with SLF4JLogging {

  override def transform(inputData: Map[String, DistributedMonad[RDD]]): DistributedMonad[RDD] =
    applyHeadTransform(inputData) { (inputSchema, inputStream) =>
      partitions.fold(inputStream.ds) { partition => inputStream.ds.repartition(partition)
      }
    }

}
