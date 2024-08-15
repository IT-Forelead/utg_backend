package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.enums.VehicleIndicatorActionType

@JsonCodec
case class TripVehicleIndicator(
    id: TripVehicleIndicatorId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    vehicleId: VehicleId,
    actionType: VehicleIndicatorActionType,
    scheduledTime: ZonedDateTime,
    currentDateTime: ZonedDateTime,
    odometerIndicator: NonNegDouble,
    spentHours: Option[NonNegDouble],
    paidDistance: NonNegDouble,
    isFulledBackAction: Boolean,
    isFulledExitAction: Boolean,
  )
