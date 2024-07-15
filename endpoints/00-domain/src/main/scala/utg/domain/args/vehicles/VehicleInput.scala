package utg.domain.args.vehicles

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.InvoiceNumber
import utg.RegisteredNumber
import utg.domain.BranchId
import utg.domain.VehicleCategoryId
import utg.domain.enums.ConditionType
import utg.domain.enums.FuelType
import utg.domain.enums.GpsTrackerType

@JsonCodec
case class VehicleInput(
    branchId: BranchId,
    vehicleCategoryId: VehicleCategoryId,
    brand: NonEmptyString,
    registeredNumber: Option[RegisteredNumber],
    invoiceNumber: InvoiceNumber,
    yearOfRelease: NonNegInt,
    bodyNumber: Option[NonEmptyString],
    chassisNumber: Option[NonEmptyString],
    engineNumber: Option[NonEmptyString],
    conditionType: ConditionType,
    fuelType: Option[FuelType],
    description: Option[NonEmptyString],
    gpsTracker: Option[GpsTrackerType],
    fuelLevelSensor: Option[NonNegDouble],
    fuelTankVolume: Option[NonNegDouble],
  )
