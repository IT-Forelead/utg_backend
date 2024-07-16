package utg.domain.args.users

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import utg.Phone
import utg.domain.{RoleId, UserId}

@JsonCodec
case class UpdateUserInput(
    userId: UserId,
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    middleName: Option[NonEmptyString],
    phone: Phone,
    branchCode: Option[String],
    roleId: RoleId,
  )
