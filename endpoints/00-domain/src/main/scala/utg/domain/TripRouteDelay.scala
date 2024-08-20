package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User

@JsonCodec
case class TripRouteDelay(
    id: TripRouteDelayId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    name: NonEmptyString,
    startTime: ZonedDateTime,
    endTime: ZonedDateTime,
    userSignature: AssetId,
    user: Option[User],
  )
