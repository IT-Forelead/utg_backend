package utg.domain.args.users

import java.time.LocalDate

import cats.data.NonEmptyList
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone
import utg.domain.AssetId
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
    personalId: Option[NonNegInt],
    birthday: Option[LocalDate],
    placeOfBirth: Option[NonEmptyString],
    address: Option[NonEmptyString],
    drivingLicenseNumber: Option[NonEmptyString],
    drivingLicenseCategories: Option[NonEmptyList[DrivingLicenseCategory]],
    drivingLicenseGiven: Option[LocalDate],
    drivingLicenseExpire: Option[LocalDate],
    drivingLicenseIssuingAuthority: Option[NonEmptyString],
    machineOperatorLicenseNumber: Option[NonEmptyString],
    machineOperatorLicenseCategories: Option[NonEmptyList[MachineOperatorLicenseCategory]],
    machineOperatorLicenseGiven: Option[LocalDate],
    machineOperatorLicenseExpire: Option[LocalDate],
    machineOperatorLicenseIssuingAuthority: Option[NonEmptyString],
    personalNumber: NonNegInt,
    phone: Phone,
    roleId: RoleId,
    branchCode: NonEmptyString,
    licensePhotoIds: Option[NonEmptyList[AssetId]],
  )
