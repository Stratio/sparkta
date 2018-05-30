/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.serving.api.actor

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, PoisonPill}
import akka.event.slf4j.SLF4JLogging
import slick.jdbc.JdbcProfile
import com.stratio.sparta.serving.api.dao.StatusHistoryDaoImpl
import com.stratio.sparta.serving.core.actor.StatusPublisherActor.StatusChange
import com.stratio.sparta.serving.core.models.history.WorkflowStatusHistory
import com.stratio.sparta.serving.core.models.workflow.WorkflowStatus
import com.stratio.sparta.serving.api.actor.StatusHistoryListenerActor._
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}

class StatusHistoryListenerActor(val profileHistory: JdbcProfile, val config: Config) extends Actor
  with StatusHistoryDaoImpl
  with SLF4JLogging{

  override val profile = profileHistory

  import profile.api._

  override val db: profile.api.Database = Database.forConfig("", config)

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[StatusChange])
    Try(db.createSession.conn) match {
      case Success(conn) =>
        createSchema()
        conn.close()
      case Failure(x) =>
        log.error(s"Unable to connect to Postgres database: ${x.getMessage}", x)
        self ! PoisonPill
    }
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self, classOf[StatusChange])
    db.close()
  }

  override def receive: Receive = {
    case sc: StatusChange => upsert(sc.workflowStatus) onFailure {
      case e: Exception => log.error("Error while upserting into status history table", e)
    }
  }
}

object StatusHistoryListenerActor {
  implicit def statusToDb(workflowStatus: WorkflowStatus): WorkflowStatusHistory = {
    WorkflowStatusHistory(
      workflowId = workflowStatus.id,
      status = workflowStatus.status.toString,
      statusId = workflowStatus.statusId,
      statusInfo = workflowStatus.statusInfo,
      creationDate = workflowStatus.creationDate.map(_.getMillis).orElse(None),
      lastUpdateDate = workflowStatus.lastUpdateDate.map(_.getMillis).orElse(None),
      lastUpdateDateWorkflow = workflowStatus.lastUpdateDateWorkflow.map(_.getMillis).orElse(None)
    )
  }
}