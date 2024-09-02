package utg.domain.args.tripFuelExpenses

import cats.data.NonEmptyList
import io.circe.generic.JsonCodec

import utg.domain.AssetId
import utg.domain.FuelTypeAndQuantity
import utg.domain.TripId
import utg.domain.enums.VehicleIndicatorActionType

@JsonCodec
case class TripFuelInspectionInput(
    tripId: TripId,
    actionType: VehicleIndicatorActionType,
    fuels: NonEmptyList[FuelTypeAndQuantity],
    mechanicSignature: AssetId,
  )
