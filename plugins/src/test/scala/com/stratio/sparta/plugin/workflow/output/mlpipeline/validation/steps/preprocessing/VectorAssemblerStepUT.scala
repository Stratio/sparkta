/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.plugin.workflow.output.mlpipeline.validation.steps.preprocessing

import com.stratio.sparta.plugin.workflow.output.mlpipeline.validation.GenericPipelineStepTest
import org.apache.spark.sql.DataFrame
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VectorAssemblerStepUT extends GenericPipelineStepTest {

  override def stepName:String  = "vectorassembler"

  override def resourcesPath:String = "/mlpipeline/singlesteps/preprocessing/vectorassembler/"

  override def trainingDf: DataFrame
  = sparkSession.createDataFrame(Seq(
    (0, "a", 0.0),
    (1, "b", 2.0),
    (2, "c", 1.0),
    (3, "a", 0.0),
    (4, "a", 0.0),
    (5, "c", 1.0)
  )).toDF("id", "category", "string_indexed")

  //override def emptyParamsAvailable: Boolean = false
  override def wrongParamsAvailable: Boolean = false
}

