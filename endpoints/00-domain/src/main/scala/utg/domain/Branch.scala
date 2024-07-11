package utg.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class Branch(
    id: BranchId,
    name: NonEmptyString,
    code: NonEmptyString,
    region: Option[Region],
  )
