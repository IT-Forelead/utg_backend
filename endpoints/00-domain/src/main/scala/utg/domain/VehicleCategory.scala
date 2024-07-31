package utg.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.enums.VehicleType

@JsonCodec
case class VehicleCategory(
    id: VehicleCategoryId,
    name: NonEmptyString,
    vehicleType: VehicleType,
  )
