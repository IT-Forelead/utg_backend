package utg.domain.args.users

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone
import utg.domain.RoleId
import utg.domain.enums.DrivingLicenseCategory

@JsonCodec
case class UserInput(
    roleId: RoleId,
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    middleName: Option[NonEmptyString],
    phone: Phone,
    branchCode: NonEmptyString,
    licenseNumber: Option[NonEmptyString],
    drivingLicenseCategories: Option[NonEmptyList[DrivingLicenseCategory]],
  )
