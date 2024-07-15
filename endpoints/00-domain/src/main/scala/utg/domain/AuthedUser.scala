package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.Phone
import utg.domain.enums.Privilege

@JsonCodec
sealed trait AuthedUser {
  val id: UserId
  val role: Role
  val firstname: NonEmptyString
  val lastname: NonEmptyString
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
      role: Role,
      phone: Phone,
      assetId: Option[AssetId],
      branchCode: Option[NonEmptyString],
    ) extends AuthedUser {
    val fullName = s"$firstname $lastname"
    def access(privilege: Privilege): Boolean = role.privileges.contains(privilege)
  }
}
