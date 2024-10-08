package utg.repos.sql

import shapeless.HNil
import skunk._
import skunk.codec.all.varchar
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.Phone
import utg.domain.UserId
import utg.domain.args.users.UserFilters
import utg.domain.args.users.UserSorting
import utg.domain.auth.AccessCredentials

private[repos] object UsersSql extends Sql[UserId] {
  private[repos] val codec =
    (id *: zonedDateTime *: nes *: nes *: nes.opt *: phone *: RolesSql.id *: AssetsSql
      .id
      .opt *: nes.opt)
      .to[dto.User]
  private val accessCredentialsDecoder: Decoder[AccessCredentials[dto.User]] =
    (codec *: passwordHash).map {
      case user *: hash *: HNil =>
        AccessCredentials(
          data = user,
          password = hash,
        )
    }

  val findByPhone: Query[Phone, AccessCredentials[dto.User]] =
    sql"""SELECT id, created_at, firstname, lastname, middle_name, phone, role_id, asset_id, branch_code, password  FROM users
          WHERE phone = $phone LIMIT 1""".query(accessCredentialsDecoder)

  val findById: Query[UserId, dto.User] =
    sql"""SELECT id, created_at, firstname, lastname, middle_name, phone, role_id, asset_id, branch_code FROM users
          WHERE id = $id LIMIT 1""".query(codec)

  def findByIds(ids: List[UserId]): Query[ids.type, dto.User] =
    sql"""SELECT id, created_at, firstname, lastname, middle_name, phone, role_id, asset_id, branch_code FROM users
          WHERE id IN (${id.values.list(ids)})""".query(codec)

  val insert: Command[AccessCredentials[dto.User]] =
    sql"""INSERT INTO users VALUES ($id, $zonedDateTime, $nes, $nes, ${nes.opt}, $phone, ${RolesSql.id}, ${AssetsSql
        .id
        .opt}, ${nes.opt}, $passwordHash)"""
      .command
      .contramap { (u: AccessCredentials[dto.User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstname *: u.data.lastname *: u.data.middleName *:
          u.data.phone *: u
            .data
            .roleId *: u.data.assetId *: u.data.branchCode *: u.password *: EmptyTuple
      }

  val update: Command[dto.User] =
    sql"""UPDATE users
       SET firstname = $nes,
       lastname = $nes,
       middle_name = ${nes.opt},
       phone = $phone,
       role_id = ${RolesSql.id},
       asset_id = ${AssetsSql.id.opt},
       branch_code = ${nes.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case user: dto.User =>
          user.firstname *: user.lastname *: user.middleName *: user.phone *: user.roleId *: user.assetId *: user.branchCode *: user.id *: EmptyTuple
      }

  val changePassword: Command[AccessCredentials[dto.User]] =
    sql"""UPDATE users
       SET password = $passwordHash,
       WHERE phone = $phone
     """
      .command
      .contramap { (u: AccessCredentials[dto.User]) =>
        u.password *: u.data.phone *: EmptyTuple
      }

  private def searchFilter(filters: UserFilters): List[Option[AppliedFragment]] =
    List(
      filters.id.map(sql"u.id = $id"),
      filters.roleId.map(sql"u.role_id = ${RolesSql.id}"),
      filters.name.map(s => s"%$s%").map(sql"u.firstname + ' ' + u.lastname ILIKE $varchar"),
    )

  private def orderBy(filters: UserFilters): Fragment[Void] = {
    val sorting: String = filters.sortBy.fold("u.created_at") {
      case UserSorting.CreatedAt => "u.created_at"
      case UserSorting.FirstName => "u.firstname"
      case UserSorting.LastName => "u.lastname"
      case UserSorting.Role => "u.role_id"
    }
    sql""" ORDER BY #$sorting #${filters.sortOrder.fold("")(_.value)}"""
  }

  def select(filters: UserFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT 
              u.id AS user_id,
              u.created_at AS created_at,
              u.firstname AS firstname,
              u.lastname  AS lastname,
              u.middle_name AS middle_name,
              u.phone AS phone,
              u.role_id AS role_id,
              u.asset_id AS asset_id,
              u.branch_code AS branch_code,
              COUNT(*) OVER() AS total
            FROM users u"""
    baseQuery(Void).whereAndOpt(searchFilter(filters)) |+| orderBy(filters)(Void)
  }

  def delete: Command[UserId] =
    sql"""DELETE FROM users u WHERE u.id = $id""".command
}
