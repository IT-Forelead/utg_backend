package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.Phone

@JsonCodec
sealed trait AuthedUser {
  val id: UserId
  val role: Role
  val firstname: NonEmptyString
  val lastname: NonEmptyString
  val login: NonEmptyString
  val phone: Phone
  val fullName: NonEmptyString
  val assetId: Option[AssetId]
}
object AuthedUser {
  @JsonCodec
  case class User(
      id: UserId,
      createdAt: ZonedDateTime,
      firstname: NonEmptyString,
      lastname: NonEmptyString,
      role: Role,
      login: NonEmptyString,
      phone: Phone,
      assetId: Option[AssetId],
    ) extends AuthedUser {
    val fullName = s"$firstname $lastname"
  }
}
