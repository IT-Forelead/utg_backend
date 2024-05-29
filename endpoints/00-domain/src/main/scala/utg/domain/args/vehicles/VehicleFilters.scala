package utg.domain.args.vehicles

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.RegisteredNumber
import utg.domain.enums.VehicleType

@JsonCodec
case class VehicleFilters(
    name: Option[NonEmptyString] = None,
    registeredNumber: Option[RegisteredNumber] = None,
    vehicleType: Option[VehicleType] = None,
    limit: Option[PosInt] = None,
    offset: Option[PosInt] = None,
  )
