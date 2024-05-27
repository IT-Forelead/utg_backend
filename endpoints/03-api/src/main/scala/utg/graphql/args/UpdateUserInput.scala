package utg.graphql.args

import caliban.uploads.Upload
import eu.timepit.refined.types.string.NonEmptyString

import utg.domain

case class UpdateUserInput(
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    upload: Option[Upload],
  ) {
  def toDomain: domain.args.users.UpdateUserInput =
    domain.args.users.UpdateUserInput(firstname, lastname)
}
