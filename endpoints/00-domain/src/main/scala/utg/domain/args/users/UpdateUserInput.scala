package utg.domain.args.users

import eu.timepit.refined.types.string.NonEmptyString

case class UpdateUserInput(
    firstname: NonEmptyString,
    lastname: NonEmptyString,
  )
