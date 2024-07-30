package utg.domain.args.completeTasks

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.CompleteTaskId
import utg.domain.ConsignorSignId
import utg.domain.DocumentId
import utg.domain.TripId

@JsonCodec
case class UpdateCompleteTaskInput(
    completeTaskId: CompleteTaskId,
    tripId: TripId,
    tripNumber: Option[NonEmptyString],
    invoiceNumber: Option[NonEmptyString],
    arrivalTime: Option[ZonedDateTime],
    consignorSignId: Option[ConsignorSignId],
    documentId: Option[DocumentId],
  )
