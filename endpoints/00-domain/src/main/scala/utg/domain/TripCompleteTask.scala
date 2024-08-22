package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User

@JsonCodec
case class TripCompleteTask(
    id: TripCompleteTaskId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    commuteNumber: NonNegInt,
    loadNumbers: NonEmptyString,
    arrivalTime: ZonedDateTime,
    consignorFullName: NonEmptyString,
    consignorSignature: AssetId,
    driver: Option[User],
  )
