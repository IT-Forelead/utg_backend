package utg.domain.args.vehicles

import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.RegisteredNumber
import utg.domain.enums.ConditionType
import utg.domain.enums.VehicleType

@JsonCodec
case class VehicleFilters(
    brand: Option[NonEmptyString] = None,
    registeredNumber: Option[RegisteredNumber] = None,
    conditionType: Option[ConditionType] = None,
    vehicleType: Option[VehicleType] = None,
    limit: Option[NonNegInt] = None,
    page: Option[NonNegInt] = None,
  )
