package utg.domain.args.tripCompleteTasks

import java.time.ZonedDateTime

import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId

@JsonCodec
case class TripCompleteTaskInput(
    tripId: TripId,
    commuteNumber: NonNegInt,
    loadNumbers: NonEmptyString,
    arrivalTime: ZonedDateTime,
    consignorFullName: NonEmptyString,
    consignorSignature: AssetId,
  )
