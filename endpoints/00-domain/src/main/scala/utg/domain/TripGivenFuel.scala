package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User

@JsonCodec
case class TripGivenFuel(
    id: TripGivenFuelId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    vehicleId: VehicleId,
    fuelBrand: NonEmptyString,
    brandCode: NonEmptyString,
    fuelGiven: NonNegDouble,
    refueler: Option[User],
    attendantSignature: AssetId,
  )