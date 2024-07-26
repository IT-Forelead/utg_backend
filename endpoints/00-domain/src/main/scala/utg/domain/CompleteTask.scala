package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class CompleteTask(
    id: CompleteTaskId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    tripNumber: Option[NonEmptyString],
    invoiceNumber: Option[NonEmptyString],
    arrivalTime: Option[ZonedDateTime],
    consignorSignId: Option[ConsignorSignId],
    documentId: Option[DocumentId],
  )
