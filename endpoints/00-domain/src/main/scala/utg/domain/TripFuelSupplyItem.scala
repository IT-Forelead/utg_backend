package utg.domain

import eu.timepit.refined.types.all.NonNegDouble
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.enums.FuelType

@JsonCodec
case class TripFuelSupplyItem(
    id: TripFuelSupplyItemId,
    tripFuelSupplyId: TripFuelSupplyId,
    fuelType: FuelType,
    fuelSupply: NonNegDouble,
  )