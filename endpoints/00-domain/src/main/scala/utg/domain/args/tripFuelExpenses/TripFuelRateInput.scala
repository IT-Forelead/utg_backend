package utg.domain.args.tripFuelExpenses

import eu.timepit.refined.types.all.NonNegDouble
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.enums.FuelType

@JsonCodec
case class TripFuelRateInput(
    tripId: TripId,
    fuelType: FuelType,
    normChangeCoefficient: NonNegDouble,
    equipmentWorkingTime: NonNegDouble,
    engineWorkingTime: NonNegDouble,
    dispatcherSignature: AssetId,
  )
