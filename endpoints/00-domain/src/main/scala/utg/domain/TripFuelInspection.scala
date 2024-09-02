package utg.domain

import java.time.ZonedDateTime

import io.circe.generic.JsonCodec

import utg.domain.AuthedUser.User
import utg.domain.enums.VehicleIndicatorActionType

@JsonCodec
case class TripFuelInspection(
    id: TripFuelInspectionId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    vehicleId: VehicleId,
    actionType: VehicleIndicatorActionType,
    fuels: List[TripFuelInspectionItem],
    mechanic: Option[User],
    mechanicSignature: AssetId,
  )
