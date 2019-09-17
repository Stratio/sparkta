/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.core.models.workflow.migration

import com.stratio.sparta.core.properties.JsoneyString
import com.stratio.sparta.serving.core.models.enumerators.DataType.DataType
import com.stratio.sparta.serving.core.models.enumerators.NodeArityEnum.NodeArity
import com.stratio.sparta.serving.core.models.enumerators.WorkflowExecutionEngine._
import com.stratio.sparta.serving.core.models.workflow.{NodeLineageProperty, NodeTemplateInfo, NodeUiConfiguration}

case class NodeGraphHydraPegaso(
                                 name: String,
                                 stepType: String,
                                 className: String,
                                 classPrettyName: String,
                                 arity: Seq[NodeArity],
                                 writer: WriterGraphHydraPegaso,
                                 description: Option[String] = None,
                                 uiConfiguration: Option[NodeUiConfiguration] = None,
                                 configuration: Map[String, JsoneyString] = Map(),
                                 nodeTemplate: Option[NodeTemplateInfo] = None,
                                 supportedEngines: Seq[ExecutionEngine] = Seq.empty[ExecutionEngine],
                                 executionEngine: Option[ExecutionEngine] = Option(Streaming),
                                 supportedDataRelations: Option[Seq[DataType]] = None,
                                 lineageProperties: Seq[NodeLineageProperty] = Seq.empty[NodeLineageProperty]
                           )