package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User
import utg.domain.enums.VehicleIndicatorActionType

@JsonCodec
case class TripFuelInspection(
    id: TripFuelInspectionId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    vehicleId: VehicleId,
    actionType: VehicleIndicatorActionType,
    fuelInTank: NonNegDouble,
    mechanic: Option[User],
    mechanicSignature: AssetId,
  )
