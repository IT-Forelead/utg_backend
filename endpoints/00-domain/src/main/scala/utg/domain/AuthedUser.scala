package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

@JsonCodec
sealed trait AuthedUser {
  val id: UserId
  val firstname: NonEmptyString
  val lastname: NonEmptyString
  val role: Role
  val login: NonEmptyString
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
      assetId: Option[AssetId],
    ) extends AuthedUser {
    val fullName = s"$firstname $lastname"
  }
}
