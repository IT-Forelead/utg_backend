package utg.domain.args.completeTasks

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import utg.domain.{ConsignorSignId, DocumentId}

import java.time.ZonedDateTime

@JsonCodec
case class CompleteTaskInput(
    tripNumber: Option[NonEmptyString],
    invoiceNumber: Option[NonEmptyString],
    arrivalTime: Option[ZonedDateTime],
    consignorSignId: Option[ConsignorSignId],
    documentId: Option[DocumentId]
  )
