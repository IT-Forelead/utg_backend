package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User

@JsonCodec
case class TripCompleteTaskAcceptance(
    id: TripCompleteTaskAcceptanceId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    commuteNumberTotal: NonNegInt,
    loadNumberTotal: NonNegInt,
    loadNumberTotalStr: NonEmptyString,
    document: Option[AssetId],
    driver: Option[User],
    driverSignature: Option[AssetId],
    dispatcher: Option[User],
    dispatcherSignature: Option[AssetId],
  )
