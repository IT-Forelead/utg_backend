package utg.domain.args.vehicles

import java.time.LocalDate

import cats.data.NonEmptyList
import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.BranchId
import utg.domain.FuelTypeAndQuantity
import utg.domain.VehicleCategoryId
import utg.domain.VehicleId
import utg.domain.enums._

@JsonCodec
case class UpdateVehicleInput(
    id: VehicleId,
    vehicleType: VehicleType,
    registeredNumber: Option[NonEmptyString],
    brand: NonEmptyString,
    color: Option[NonEmptyString],
    owner: Option[NonEmptyString],
    address: Option[NonEmptyString],
    dateOfIssue: Option[LocalDate],
    issuingAuthority: Option[NonEmptyString],
    pin: Option[NonNegInt],
    yearOfRelease: NonNegInt,
    vehicleCategoryId: VehicleCategoryId,
    bodyNumber: Option[NonEmptyString],
    chassisNumber: Option[NonEmptyString],
    maxMass: NonNegInt,
    unloadMass: NonNegInt,
    engineNumber: Option[NonEmptyString],
    engineCapacity: Option[NonNegInt],
    fuels: Option[NonEmptyList[FuelTypeAndQuantity]],
    numberOfSeats: NonNegInt,
    numberOfStandingPlaces: NonNegInt,
    specialMarks: Option[NonEmptyString],
    licenseNumber: Option[NonEmptyString],
    branchId: BranchId,
    conditionType: ConditionType,
    gpsTracking: Option[GpsTrackingType],
    fuelLevelSensor: Option[NonNegDouble],
    description: Option[NonEmptyString],
    vehiclePhotoIds: Option[NonEmptyList[AssetId]],
    licensePhotoIds: Option[NonEmptyList[AssetId]],
  )
