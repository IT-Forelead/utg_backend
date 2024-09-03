package utg.domain.args.tripFuelSupplies

import cats.data.NonEmptyList
import io.circe.generic.JsonCodec

import utg.domain.AssetId
import utg.domain.FuelTypeAndQuantity
import utg.domain.TripId

@JsonCodec
case class TripFuelSupplyInput(
    tripId: TripId,
    fuelSupplies: NonEmptyList[FuelTypeAndQuantity],
    dispatcherSignature: AssetId,
  )
