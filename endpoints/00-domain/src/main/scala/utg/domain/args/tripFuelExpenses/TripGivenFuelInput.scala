package utg.domain.args.tripFuelExpenses

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.enums.FuelType

@JsonCodec
case class TripGivenFuelInput(
    tripId: TripId,
    fuelBrand: FuelType,
    brandCode: Option[NonEmptyString],
    fuelGiven: NonNegDouble,
    paymentCheckId: Option[AssetId],
    refuelerSignature: AssetId,
  )
