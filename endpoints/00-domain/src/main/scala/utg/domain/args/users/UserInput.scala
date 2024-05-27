package utg.domain.args.users

import eu.timepit.refined.types.string.NonEmptyString

import utg.Phone
import utg.domain.RoleId

case class UserInput(
    roleId: RoleId,
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    login: NonEmptyString,
    phone: Phone,
  )
