package utg.domain.args.vehicles

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

import utg.RegisteredNumber
import utg.domain.enums.VehicleType

case class VehicleFilters(
    name: Option[NonEmptyString] = None,
    registeredNumber: Option[RegisteredNumber] = None,
    vehicleType: Option[VehicleType] = None,
    limit: Option[PosInt] = None,
    offset: Option[PosInt] = None,
  )
