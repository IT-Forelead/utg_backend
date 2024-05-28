package utg.domain.args.vehicles

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString

import utg.RegisteredNumber
import utg.domain.enums.VehicleType

case class VehicleInput(
    name: NonEmptyString,
    registeredNumber: RegisteredNumber,
    vehicleType: VehicleType,
    fuelTankVolume: NonNegDouble,
  )
