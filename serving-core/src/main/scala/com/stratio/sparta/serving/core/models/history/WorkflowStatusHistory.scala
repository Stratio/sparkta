/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.core.models.history

case class WorkflowStatusHistory(workflowId: String,
                                 status: String,
                                 statusId: Option[String] = None,
                                 statusInfo: Option[String] = None,
                                 creationDate: Option[Long] = None,
                                 lastUpdateDate: Option[Long] = None,
                                 lastUpdateDateWorkflow: Option[Long] = None)