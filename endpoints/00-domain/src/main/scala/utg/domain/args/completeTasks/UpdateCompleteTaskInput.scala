package utg.domain.args.completeTasks

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import utg.domain.{CompleteTaskId, ConsignorSignId, DocumentId, TripId}

import java.time.ZonedDateTime

@JsonCodec
case class UpdateCompleteTaskInput(
    completeTaskId: CompleteTaskId,
    tripId: TripId,
    tripNumber: Option[NonEmptyString],
    invoiceNumber: Option[NonEmptyString],
    arrivalTime: Option[ZonedDateTime],
    consignorSignId: Option[ConsignorSignId],
    documentId: Option[DocumentId]
  )
