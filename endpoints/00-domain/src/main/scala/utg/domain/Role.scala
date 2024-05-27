package utg.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import utg.domain.RoleId
import utg.domain.enums.Privilege

@JsonCodec
case class Role(
    id: RoleId,
    name: NonEmptyString,
    privileges: List[Privilege],
  )
