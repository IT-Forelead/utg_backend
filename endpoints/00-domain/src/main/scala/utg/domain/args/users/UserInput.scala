package utg.domain.args.users

import java.time.LocalDate

import cats.data.NonEmptyList
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone
import utg.domain.RoleId
import utg.domain.enums.DrivingLicenseCategory
import utg.domain.enums.MachineOperatorLicenseCategory

@JsonCodec
case class UserInput(
    roleId: RoleId,
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    middleName: Option[NonEmptyString],
    personalNumber: NonNegInt,
    phone: Phone,
    branchCode: NonEmptyString,
    drivingLicenseNumber: Option[NonEmptyString],
    drivingLicenseCategories: Option[NonEmptyList[DrivingLicenseCategory]],
    machineOperatorLicenseNumber: Option[NonEmptyString],
    machineOperatorLicenseCategories: Option[NonEmptyList[MachineOperatorLicenseCategory]],
    birthday: Option[LocalDate],
    drivingLicenseGiven: Option[LocalDate],
    drivingLicenseExpire: Option[LocalDate],
    machineOperatorLicenseGiven: Option[LocalDate],
    machineOperatorLicenseExpire: Option[LocalDate],
  )
