package utg.domain.args.users

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone

@JsonCodec
case class UpdateUserInput(
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    phone: Phone,
  )
