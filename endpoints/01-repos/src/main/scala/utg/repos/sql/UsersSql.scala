package utg.repos.sql

import shapeless.HNil
import skunk._
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.EmailAddress
import utg.domain.UserId
import utg.domain.args.users.UserFilters
import utg.domain.auth.AccessCredentials

private[repos] object UsersSql extends Sql[UserId] {
  private[repos] val codec =
    (id *: zonedDateTime *: nes *: nes *: nes *: RolesSql.id *: AssetsSql.id.opt).to[dto.User]
  private val accessCredentialsDecoder: Decoder[AccessCredentials[dto.User]] =
    (codec *: passwordHash).map {
      case user *: hash *: HNil =>
        AccessCredentials(
          data = user,
          password = hash,
        )
    }

  val findByLogin: Query[EmailAddress, AccessCredentials[dto.User]] =
    sql"""SELECT id, created_at, firstname, lastname, login, role_id, asset_id, password FROM users
          WHERE email = $email LIMIT 1""".query(accessCredentialsDecoder)

  val findById: Query[UserId, dto.User] =
    sql"""SELECT id, created_at, firstname, lastname, login, role_id, asset_id FROM users
          WHERE id = $id LIMIT 1""".query(codec)

  def findByIds(ids: List[UserId]): Query[ids.type, dto.User] =
    sql"""SELECT id, created_at, firstname, lastname, login, role_id, asset_id FROM users
          WHERE id IN (${id.values.list(ids)})""".query(codec)

  val insert: Command[AccessCredentials[dto.User]] =
    sql"""INSERT INTO users VALUES ($id, $zonedDateTime, $nes, $nes, $nes, ${RolesSql.id}, ${AssetsSql.id.opt}, $passwordHash)"""
      .command
      .contramap { (u: AccessCredentials[dto.User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstname *: u.data.lastname *: u.data.login *:
          u.data.roleId *: u.data.assetId *: u.password *: EmptyTuple
      }

  val update: Command[dto.User] =
    sql"""UPDATE users
       SET firstname = $nes,
       lastname = $nes,
       role_id = ${RolesSql.id},
       asset_id = ${AssetsSql.id.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case user: dto.User =>
          user.firstname *: user.lastname *: user.roleId *: user.assetId *: user.id *: EmptyTuple

      }

  private def searchFilter(filters: UserFilters): List[Option[AppliedFragment]] =
    List(
      filters.id.map(sql"u.id = $id")
    )

  def select(filters: UserFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT u.id, u.created_at, u.firstname, u.lastname, u.login, u.role_id, u.asset_id, COUNT(*) OVER() AS total FROM users u"""
    baseQuery(Void).whereAndOpt(searchFilter(filters))
  }
}
