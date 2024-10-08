package utg.domain.args.users

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class CreateRoleInput(
    name: NonEmptyString
  )
