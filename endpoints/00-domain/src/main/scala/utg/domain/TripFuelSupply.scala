package utg.domain

import java.time.ZonedDateTime

import io.circe.generic.JsonCodec

import utg.domain.AuthedUser.User

@JsonCodec
case class TripFuelSupply(
    id: TripFuelSupplyId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    vehicleId: VehicleId,
    fuelSupplies: List[TripFuelSupplyItem],
    dispatcher: Option[User],
    dispatcherSignature: AssetId,
  )
