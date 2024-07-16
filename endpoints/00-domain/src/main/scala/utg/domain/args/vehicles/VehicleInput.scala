package utg.domain.args.vehicles

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.InventoryNumber
import utg.RegisteredNumber
import utg.domain.BranchId
import utg.domain.VehicleCategoryId
import utg.domain.enums._

@JsonCodec
case class VehicleInput(
    branchId: BranchId,
    vehicleCategoryId: VehicleCategoryId,
    vehicleType: VehicleType,
    brand: NonEmptyString,
    registeredNumber: Option[RegisteredNumber],
    inventoryNumber: InventoryNumber,
    yearOfRelease: NonNegInt,
    bodyNumber: Option[NonEmptyString],
    chassisNumber: Option[NonEmptyString],
    engineNumber: Option[NonEmptyString],
    conditionType: ConditionType,
    fuelType: Option[FuelType],
    description: Option[NonEmptyString],
    gpsTracking: Option[GpsTrackingType],
    fuelLevelSensor: Option[NonNegDouble],
    fuelTankVolume: Option[NonNegDouble],
  )
