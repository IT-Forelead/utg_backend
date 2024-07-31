package utg.domain.args.tripVehicleIndicators

import java.time.ZonedDateTime

import eu.timepit.refined.types.numeric.NonNegDouble
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.TripId
import utg.domain.enums._

@JsonCodec
case class TripVehicleIndicatorInput(
    tripId: TripId,
    actionType: VehicleIndicatorActionType,
    scheduledTime: ZonedDateTime,
    currentDateTime: ZonedDateTime,
    odometerIndicator: NonNegDouble,
    paidDistance: Option[NonNegDouble],
  )
