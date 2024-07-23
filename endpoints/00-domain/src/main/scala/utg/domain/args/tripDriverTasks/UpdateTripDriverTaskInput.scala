package utg.domain.args.tripDriverTasks

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.TripDriverTaskId
import utg.domain.TripId

@JsonCodec
case class UpdateTripDriverTaskInput(
    tripDriverTaskId: TripDriverTaskId,
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
