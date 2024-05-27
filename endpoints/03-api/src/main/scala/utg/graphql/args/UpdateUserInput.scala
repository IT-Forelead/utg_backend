package utg.graphql.args

import caliban.uploads.Upload
import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.chimney.dsl._

import utg.Phone
import utg.domain

case class UpdateUserInput(
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    phone: Phone,
    upload: Option[Upload],
  ) {
  def toDomain: domain.args.users.UpdateUserInput =
    this.transformInto[domain.args.users.UpdateUserInput]
}
