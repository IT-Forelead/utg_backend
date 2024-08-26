package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class TripDriverTask(
    id: TripDriverTaskId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    whoseDiscretion: NonEmptyString,
    arrivalTime: Option[ZonedDateTime],
    pickupLocation: NonEmptyString,
    deliveryLocation: NonEmptyString,
    freightName: NonEmptyString,
    numberOfInteractions: Option[NonNegInt],
    distance: Option[NonNegDouble],
    freightVolume: Option[NonNegDouble],
  )
