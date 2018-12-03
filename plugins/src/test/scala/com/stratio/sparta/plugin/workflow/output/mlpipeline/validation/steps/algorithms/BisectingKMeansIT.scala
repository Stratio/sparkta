/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.output.mlpipeline.validation.steps.algorithms

import com.stratio.sparta.plugin.workflow.output.mlpipeline.validation.GenericPipelineStepTest
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.DataFrame
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BisectingKMeansIT extends GenericPipelineStepTest {


  override def stepName: String = "bisectingkmeans"

  override def resourcesPath: String = "/mlpipeline/singlesteps/algorithms/bisectingkmeans/"

  override def trainingDf: DataFrame = {
    val rdd = sparkSession.sparkContext.parallelize(1 to 150).map(i => Vectors.dense(Array.fill(3)((i % 7).toDouble)))
      .map(v => new TestRow(v))
    sparkSession.createDataFrame(rdd)
  }
}