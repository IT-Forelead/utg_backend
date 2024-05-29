package utg.domain.args.users

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone
import utg.domain.RoleId

@JsonCodec
case class UserInput(
    roleId: RoleId,
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    login: NonEmptyString,
    phone: Phone,
  )
