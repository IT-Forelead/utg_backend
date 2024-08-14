package utg.domain.args.tripFuelExpenses

import eu.timepit.refined.types.all.NonNegDouble
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.enums.VehicleIndicatorActionType

@JsonCodec
case class TripFuelInspectionInput(
    tripId: TripId,
    actionType: VehicleIndicatorActionType,
    fuelInTank: NonNegDouble,
    mechanicSignature: AssetId,
  )
