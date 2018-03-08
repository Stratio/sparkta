/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package com.stratio.sparta.dg.agent.model

import play.api.libs.json.{JsObject, Json, Reads, Writes}
import com.stratio.governance.commons.agent.model.metadata._
import com.stratio.governance.commons.agent.model.metadata.lineage.EventType.EventType
import com.stratio.governance.commons.agent.model.metadata.lineage.{EventType, StatusMetadata}
import org.joda.time.DateTime

case class SpartaWorkflowStatusMetadata(
                                         name: String,
                                         status: EventType,
                                         error: Option[String],
                                         key: String,
                                         metadataPath: MetadataPath,
                                         agentVersion: String = SpartaType.agentVersion,
                                         serverVersion: String = SpartaType.serverVersion,
                                         tags: List[String],
                                         modificationTime: Option[Long] = Some(System.currentTimeMillis()),
                                         accessTime: Option[Long] = Some(System.currentTimeMillis()),
                                         operationCommandType: OperationCommandType = OperationCommandType.ALTER,
                                         genericType: GenericType = GenericType.STATUS,
                                         customType: CustomType = SpartaType.STATUS,
                                         sourceType: SourceType = SourceType.SPARTA
                                       ) extends StatusMetadata {

  def serialize: String =
    Json.stringify(
      JsObject(
        Json.toJson(this).as[JsObject].fieldSet.map {
          case (key, value) => (camelToUnderscores(key), value)
        }.toMap
      )
    )

  def asMap: Map[String, Any] = this.getClass.getDeclaredFields
    .map(_.getName) // all field names
    .zip(this.productIterator.to).map(t => t._2 match {
    case mt: MetadataType => (t._1, Some(mt.value))
    case None => (t._1, None)
    case Some(_) => (t._1, t._2)
    case _ => (t._1, Some(t._2))
  }).toMap.collect {
    case (key, Some(value)) => camelToUnderscores(key) -> value
  }

  def toJson: JsObject = Json.toJson(this).as[JsObject]

  override def metadataCopy(operationCommandType: OperationCommandType): Metadata =
    this.copy(operationCommandType = operationCommandType)
}

object SpartaWorkflowStatusMetadata {

  implicit val writes: Writes[SpartaWorkflowStatusMetadata] = Json.writes[SpartaWorkflowStatusMetadata]
  implicit val readsEvent = Reads.enumNameReads(EventType)
  implicit val reads: Reads[SpartaWorkflowStatusMetadata] = Json.reads[SpartaWorkflowStatusMetadata]
}