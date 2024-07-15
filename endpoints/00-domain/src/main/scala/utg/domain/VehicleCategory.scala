package utg.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class VehicleCategory(
    id: VehicleCategoryId,
    name: NonEmptyString,
  )
