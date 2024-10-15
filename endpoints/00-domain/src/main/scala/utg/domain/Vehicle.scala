package utg.domain

import java.time.LocalDate
import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.InventoryNumber
import utg.domain.enums._

@JsonCodec
case class Vehicle(
    id: VehicleId,
    createdAt: ZonedDateTime,
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
    vehicleCategory: Option[VehicleCategory],
    bodyNumber: Option[NonEmptyString],
    chassisNumber: Option[NonEmptyString],
    maxMass: NonNegInt,
    unloadMass: NonNegInt,
    engineNumber: Option[NonEmptyString],
    engineCapacity: Option[NonNegInt],
    fuels: List[VehicleFuelItem],
    numberOfSeats: NonNegInt,
    numberOfStandingPlaces: NonNegInt,
    specialMarks: Option[NonEmptyString],
    licenseNumber: Option[NonEmptyString],
    branch: Option[Branch],
    inventoryNumber: InventoryNumber,
    conditionType: ConditionType,
    gpsTracking: Option[GpsTrackingType],
    fuelLevelSensor: Option[NonNegDouble],
    description: Option[NonEmptyString],
  )
