package utg.domain

import java.time.ZonedDateTime

import cats.data.NonEmptyList
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.Phone
import utg.domain.enums.DrivingLicenseCategory
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
  val assetId: Option[AssetId]
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
      personalNumber: NonNegInt,
      role: Role,
      phone: Phone,
      assetId: Option[AssetId],
      branch: Option[Branch],
      drivingLicenseNumber: Option[NonEmptyString],
      drivingLicenseCategories: Option[NonEmptyList[DrivingLicenseCategory]],
    ) extends AuthedUser {
    val fullName = s"$firstname $lastname"
    def access(privilege: Privilege): Boolean = role.privileges.contains(privilege)
  }
}
