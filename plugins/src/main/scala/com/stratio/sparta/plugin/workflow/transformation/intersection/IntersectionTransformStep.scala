/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.transformation.intersection

import java.io.{Serializable => JSerializable}

import akka.event.slf4j.SLF4JLogging
import com.stratio.sparta.sdk.DistributedMonad
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._
import com.stratio.sparta.sdk.workflow.step.{OutputOptions, TransformStep, TransformationStepManagement}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.crossdata.XDSession
import org.apache.spark.streaming.StreamingContext

abstract class IntersectionTransformStep[Underlying[Row]](
                                                           name: String,
                                                           outputOptions: OutputOptions,
                                                           transformationStepsManagement: TransformationStepManagement,
                                                           ssc: Option[StreamingContext],
                                                           xDSession: XDSession,
                                                           properties: Map[String, JSerializable]
                                                         )(implicit dsMonadEvidence: Underlying[Row] => DistributedMonad[Underlying])
  extends TransformStep[Underlying](name, outputOptions, transformationStepsManagement, ssc, xDSession, properties) {

  lazy val partitions = properties.getInt("partitions", None)

  def transformFunc: (RDD[Row], RDD[Row]) => RDD[Row] = {
    case (rdd1, rdd2) =>
      partitions.fold(rdd1.intersection(rdd2)) { numPartitions =>
        rdd1.intersection(rdd2, numPartitions)
      }
  }
}

