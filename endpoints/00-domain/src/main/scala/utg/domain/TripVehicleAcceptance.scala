package utg.domain

import java.time.ZonedDateTime

import io.circe.generic.JsonCodec

import utg.domain.AuthedUser.User
import utg.domain.enums.ConditionType
import utg.domain.enums.VehicleIndicatorActionType

@JsonCodec
case class TripVehicleAcceptance(
    id: TripVehicleAcceptanceId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    vehicleId: VehicleId,
    actionType: VehicleIndicatorActionType,
    conditionType: ConditionType,
    mechanic: Option[User],
    mechanicSignature: Option[AssetId],
    driver: Option[User],
    driverSignature: Option[AssetId],
  )
