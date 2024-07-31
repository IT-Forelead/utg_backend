package utg.domain.args.vehicleCategories

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.enums.VehicleType

@JsonCodec
case class VehicleCategoryFilters(
    name: Option[NonEmptyString] = None,
    vehicleType: Option[VehicleType] = None,
  )
