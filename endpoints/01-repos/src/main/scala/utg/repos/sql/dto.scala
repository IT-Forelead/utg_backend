package utg.repos.sql

import java.time.ZonedDateTime
import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.chimney.dsl._
import uz.scala.syntax.refined._
import utg.Phone
import utg.domain
import utg.domain.{AssetId, AuthedUser, BranchId, RegionId, RoleId, UserId}

object dto {
  case class User(
      id: UserId,
      createdAt: ZonedDateTime,
      firstname: NonEmptyString,
      lastname: NonEmptyString,
      phone: Phone,
      roleId: RoleId,
      assetId: Option[AssetId],
    ) {
    def toDomain(role: domain.Role): AuthedUser.User =
      this
        .into[AuthedUser.User]
        .withFieldConst(_.role, role)
        .withFieldConst(_.phone, phone)
        .transform
  }

  object User {
    def fromDomain(user: AuthedUser.User): User =
      user
        .into[User]
        .withFieldConst(_.roleId, user.role.id)
        .transform
  }

  case class Role(id: RoleId, name: NonEmptyString)
  case class Region(
      id: RegionId,
      name: NonEmptyString,
      deleted: Boolean = false,
    )
  case class Branch(
      id: BranchId,
      name: NonEmptyString,
      code: NonEmptyString,
      regionId: RegionId,
      deleted: Boolean = false,
    )
}
