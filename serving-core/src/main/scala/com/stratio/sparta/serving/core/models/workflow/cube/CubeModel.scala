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
package com.stratio.sparta.serving.core.models.workflow.cube

import com.stratio.sparta.sdk.properties.JsoneyString
import com.stratio.sparta.serving.core.models.workflow.writer.WriterModel
import com.stratio.sparta.serving.core.models.workflow.trigger.TriggerModel
import com.stratio.sparta.sdk.properties.ValidatingPropertyMap._

import scala.util.Try

case class CubeModel(name: String,
                     dimensions: Seq[DimensionModel],
                     operators: Seq[OperatorModel],
                     writer: WriterModel,
                     triggers: Seq[TriggerModel] = Seq.empty[TriggerModel],
                     configuration: Map[String, JsoneyString] = Map.empty
                    ) {

  def avoidNullValues: Boolean =
    Try(configuration.getBoolean("avoidNullValues")).getOrElse(true)

  def rememberPartitioner: Boolean =
    Try(configuration.getBoolean("rememberPartitioner")).getOrElse(true)
}
