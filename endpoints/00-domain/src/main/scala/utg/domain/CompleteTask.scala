package utg.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec

import java.time.ZonedDateTime

@JsonCodec
case class CompleteTask(
    id: CompleteTaskId,
    tripNumber: Option[NonEmptyString],
    invoiceNumber: Option[NonEmptyString],
    arrivalTime: Option[ZonedDateTime],
    consignorSignId: Option[ConsignorSignId],
    documentId: Option[DocumentId]
  )
