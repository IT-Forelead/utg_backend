package utg.domain.args.tripDrivers

import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.TripId
import utg.domain.UserId
import utg.domain.enums.HealthType

@JsonCodec
case class TripDriverFilters(
    tripId: Option[TripId] = None,
    driverId: Option[UserId] = None,
    driverHealth: Option[HealthType] = None,
    doctorId: Option[UserId] = None,
    isVerified: Option[Boolean] = None,
    limit: Option[NonNegInt] = None,
    offset: Option[NonNegInt] = None,
  )
