package utg.domain.args.tripDriverTasks

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import utg.domain.TripId

import java.time.ZonedDateTime
import io.circe.refined._

@JsonCodec
case class TripDriverTaskInput(
    tripId: TripId,
    whoseDiscretion: NonEmptyString,
    arrivalTime: ZonedDateTime,
    pickupLocation: NonEmptyString,
    deliveryLocation: NonEmptyString,
    freightName: NonEmptyString,
    numberOfInteractions: NonNegInt,
    distance: NonNegDouble,
    freightVolume: NonNegDouble,
  )
