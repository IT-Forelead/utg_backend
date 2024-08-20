package utg.domain.args.tripRouteDelays

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId

@JsonCodec
case class TripRouteDelayInput(
    tripId: TripId,
    name: NonEmptyString,
    startTime: ZonedDateTime,
    endTime: ZonedDateTime,
    userSignature: AssetId,
  )
