package utg.domain.args.vehicles

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.RegisteredNumber
import utg.domain.enums.VehicleType

@JsonCodec
case class VehicleInput(
    name: NonEmptyString,
    registeredNumber: RegisteredNumber,
    vehicleType: VehicleType,
    fuelTankVolume: NonNegDouble,
  )
