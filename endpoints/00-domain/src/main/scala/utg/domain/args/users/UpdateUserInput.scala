package utg.domain.args.users

import eu.timepit.refined.types.string.NonEmptyString

import utg.Phone

case class UpdateUserInput(
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    phone: Phone,
  )
