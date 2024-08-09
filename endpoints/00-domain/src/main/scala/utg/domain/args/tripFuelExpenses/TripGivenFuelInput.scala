package utg.domain.args.tripFuelExpenses

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId

@JsonCodec
case class TripGivenFuelInput(
    tripId: TripId,
    fuelBrand: NonEmptyString,
    brandCode: NonEmptyString,
    fuelGiven: NonNegDouble,
    attendantSignature: AssetId,
  )
