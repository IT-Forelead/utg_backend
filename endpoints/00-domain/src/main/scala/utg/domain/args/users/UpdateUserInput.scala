package utg.domain.args.users

import cats.data.NonEmptyList
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone
import utg.domain.RoleId
import utg.domain.UserId
import utg.domain.enums.DrivingLicenseCategory
import utg.domain.enums.MachineOperatorLicenseCategory

@JsonCodec
case class UpdateUserInput(
    userId: UserId,
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    middleName: Option[NonEmptyString],
    personalNumber: NonNegInt,
    phone: Phone,
    branchCode: Option[String],
    roleId: RoleId,
    drivingLicenseNumber: Option[NonEmptyString],
    drivingLicenseCategories: Option[NonEmptyList[DrivingLicenseCategory]],
    machineOperatorLicenseNumber: Option[NonEmptyString],
    machineOperatorLicenseCategories: Option[NonEmptyList[MachineOperatorLicenseCategory]],
  )
