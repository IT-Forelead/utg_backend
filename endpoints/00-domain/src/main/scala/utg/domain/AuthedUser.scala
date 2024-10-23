package utg.domain

import java.time.LocalDate
import java.time.ZonedDateTime

import cats.data.NonEmptyList
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.numeric.NonNegLong
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.Phone
import utg.domain.Asset.AssetInfo
import utg.domain.enums.DrivingLicenseCategory
import utg.domain.enums.MachineOperatorLicenseCategory
import utg.domain.enums.Privilege

@JsonCodec
sealed trait AuthedUser {
  val id: UserId
  val role: Role
  val firstname: NonEmptyString
  val lastname: NonEmptyString
  val personalNumber: NonNegInt
  val phone: Phone
  val fullName: NonEmptyString
  def access(privilege: Privilege): Boolean
}

object AuthedUser {
  @JsonCodec
  case class User(
      id: UserId,
      createdAt: ZonedDateTime,
      firstname: NonEmptyString,
      lastname: NonEmptyString,
      middleName: Option[NonEmptyString],
      personalId: Option[NonNegLong],
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
      role: Role,
      branch: Option[Branch],
      licensePhotoIds: List[AssetId],
    ) extends AuthedUser {
    val fullName = s"$firstname $lastname"
    def access(privilege: Privilege): Boolean = role.privileges.contains(privilege)
  }

  @JsonCodec
  case class UserInfo(
      id: UserId,
      createdAt: ZonedDateTime,
      firstname: NonEmptyString,
      lastname: NonEmptyString,
      middleName: Option[NonEmptyString],
      fullName: NonEmptyString,
      personalId: Option[NonNegLong],
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
      role: Role,
      branch: Option[Branch],
      licensePhotos: List[AssetInfo],
    )
}
