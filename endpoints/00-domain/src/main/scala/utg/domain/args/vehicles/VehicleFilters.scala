package utg.domain.args.vehicles

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.RegisteredNumber
import utg.domain.enums.ConditionType

@JsonCodec
case class VehicleFilters(
    brand: Option[NonEmptyString] = None,
    registeredNumber: Option[RegisteredNumber] = None,
    conditionType: Option[ConditionType] = None,
    limit: Option[PosInt] = None,
    page: Option[PosInt] = None,
  )
