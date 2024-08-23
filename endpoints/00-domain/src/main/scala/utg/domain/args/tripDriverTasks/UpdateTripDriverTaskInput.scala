package utg.domain.args.tripDriverTasks

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.TripDriverTaskId

@JsonCodec
case class UpdateTripDriverTaskInput(
    id: TripDriverTaskId,
    whoseDiscretion: Option[NonEmptyString],
    arrivalTime: Option[ZonedDateTime],
    pickupLocation: Option[NonEmptyString],
    deliveryLocation: Option[NonEmptyString],
    freightName: Option[NonEmptyString],
    numberOfInteractions: Option[NonNegInt],
    distance: Option[NonNegDouble],
    freightVolume: Option[NonNegDouble],
  )
