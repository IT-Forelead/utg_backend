package utg.domain.args.tripFuelExpenses

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripFuelExpenseId
import utg.domain.TripId
import utg.domain.UserId

@JsonCodec
case class UpdateTripFuelExpenseInput(
    id: TripFuelExpenseId,
    tripId: TripId,
    fuelBrand: Option[NonEmptyString],
    brandCode: Option[NonEmptyString],
    fuelGiven: Option[NonNegDouble],
    fuelAttendant: Option[NonEmptyString],
    attendantSignature: Option[AssetId],
    fuelInTank: Option[NonNegDouble],
    fuelRemaining: Option[NonNegDouble],
    normChangeCoefficient: Option[NonNegDouble],
    equipmentWorkingTime: Option[NonNegDouble],
    engineWorkingTime: Option[NonNegDouble],
    tankCheckMechanicId: Option[UserId],
    tankCheckMechanicSignature: Option[AssetId],
    remainingCheckMechanicId: Option[UserId],
    remainingCheckMechanicSignature: Option[AssetId],
    dispatcherId: Option[UserId],
    dispatcherSignature: Option[AssetId],
  )
