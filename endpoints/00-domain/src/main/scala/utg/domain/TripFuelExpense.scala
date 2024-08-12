package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User

@JsonCodec
case class TripFuelExpense(
    id: TripFuelExpenseId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    vehicleId: VehicleId,
    fuelBrand: Option[NonEmptyString],
    brandCode: Option[NonEmptyString],
    fuelGiven: Option[NonNegDouble],
    refueler: Option[User],
    attendantSignature: Option[AssetId],
    fuelInTank: Option[NonNegDouble],
    fuelRemaining: Option[NonNegDouble],
    normChangeCoefficient: Option[NonNegDouble],
    equipmentWorkingTime: Option[NonNegDouble],
    engineWorkingTime: Option[NonNegDouble],
    tankCheckMechanic: Option[User],
    tankCheckMechanicSignature: Option[AssetId],
    remainingCheckMechanic: Option[User],
    remainingCheckMechanicSignature: Option[AssetId],
    dispatcher: Option[User],
    dispatcherSignature: Option[AssetId],
  )
