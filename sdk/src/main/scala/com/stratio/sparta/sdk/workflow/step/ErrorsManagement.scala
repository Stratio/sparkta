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

import com.stratio.sparta.sdk.workflow.enumerators.WhenError.WhenError
import com.stratio.sparta.sdk.workflow.enumerators.WhenFieldError.WhenFieldError
import com.stratio.sparta.sdk.workflow.enumerators.{WhenError, WhenFieldError, WhenRowError}
import com.stratio.sparta.sdk.workflow.enumerators.WhenRowError.WhenRowError

case class ErrorsManagement(
                             genericErrorManagement: GenericManagement = GenericManagement(),
                             transformationStepsManagement: TransformationStepManagement = TransformationStepManagement(),
                             transactionsManagement: TransactionsManagement = TransactionsManagement()
                           )

case class GenericManagement(
                              whenError: WhenError.Value = WhenError.Error
                            )

case class TransformationStepManagement(
                                         whenError: WhenError = WhenError.Error,
                                         whenRowError: WhenRowError = WhenRowError.RowError,
                                         whenFieldError: WhenFieldError = WhenFieldError.FieldError
                                       )

case class TransactionsManagement(
                                   sendToOutputs: Seq[ErrorOutputAction] = Seq.empty,
                                   sendStepData: Boolean = false,
                                   sendPredecessorsData: Boolean = false,
                                   sendInputData: Boolean = true
                                 )

case class ErrorOutputAction(
                              outputStepName: String,
                              omitSaveErrors: Boolean = true,
                              addRedirectDate: Boolean = false,
                              redirectDateColName: Option[String] = None
                            )