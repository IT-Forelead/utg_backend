package utg.repos.sql

import shapeless.HNil
import skunk._
import skunk.codec.all.varchar
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.Phone
import utg.domain.UserId
import utg.domain.args.users.UserFilters
import utg.domain.args.users.UserSorting
import utg.domain.auth.AccessCredentials

private[repos] object UsersSql extends Sql[UserId] {
  private[repos] val codec =
    (id *: zonedDateTime *: nes *: nes *: nes.opt *: nonNegInt *: phone *: RolesSql.id
      *: AssetsSql.id.opt *: nes.opt *: nes.opt *: drivingLicenseCategories.opt *: nes.opt
      *: machineOperatorLicenseCategory.opt)
      .to[dto.User]

  private val accessCredentialsDecoder: Decoder[AccessCredentials[dto.User]] =
    (id *: zonedDateTime *: nes *: nes *: nes.opt *: nonNegInt *: phone *: RolesSql.id
      *: AssetsSql.id.opt *: nes.opt *: nes.opt *: drivingLicenseCategories.opt *: passwordHash
      *: nes.opt *: machineOperatorLicenseCategory.opt).map {
      case id *: createdAt *: firstname *: lastname *: middleName *: personalNumber *: phone *: roleId *: assetId *:
        branchCode *: drivingLicenseNumber *: drivingLicenseCategories *: hash *: machineOperatorLicenseNumber *:
        machineOperatorLicenseCategories *: HNil =>
        AccessCredentials(
          data = dto.User(
            id = id,
            createdAt = createdAt,
            firstname = firstname,
            lastname = lastname,
            middleName = middleName,
            personalNumber = personalNumber,
            phone = phone,
            roleId = roleId,
            assetId = assetId,
            branchCode = branchCode,
            drivingLicenseNumber = drivingLicenseNumber,
            drivingLicenseCategories = drivingLicenseCategories,
            machineOperatorLicenseNumber = machineOperatorLicenseNumber,
            machineOperatorLicenseCategories = machineOperatorLicenseCategories,
          ),
          password = hash,
        )
    }

  val findByPhone: Query[Phone, AccessCredentials[dto.User]] =
    sql"""SELECT * FROM users WHERE phone = $phone LIMIT 1""".query(accessCredentialsDecoder)

  val findById: Query[UserId, dto.User] =
    sql"""SELECT
          id,
          created_at,
          firstname,
          lastname,
          middle_name,
          personal_number,
          phone,
          role_id,
          asset_id,
          branch_code,
          driving_license_number,
          driving_license_categories,
          machine_operator_license_number,
          machine_operator_license_category
          FROM users
          WHERE id = $id LIMIT 1""".query(codec)

  def findByIds(ids: List[UserId]): Query[ids.type, dto.User] =
    sql"""SELECT
          id,
          created_at,
          firstname,
          lastname,
          middle_name,
          personal_number,
          phone,
          role_id,
          asset_id,
          branch_code,
          driving_license_number,
          driving_license_categories,
          machine_operator_license_number,
          machine_operator_license_category
          FROM users
          WHERE id IN (${id.values.list(ids)})""".query(codec)

  val insert: Command[AccessCredentials[dto.User]] =
    sql"""INSERT INTO users VALUES (
          $id,
          $zonedDateTime,
          $nes,
          $nes,
          ${nes.opt},
          $nonNegInt,
          $phone,
          ${RolesSql.id},
          ${AssetsSql.id.opt},
          ${nes.opt},
          ${nes.opt},
          ${drivingLicenseCategories.opt},
          $passwordHash,
          ${nes.opt},
          ${machineOperatorLicenseCategory.opt}
        )"""
      .command
      .contramap { (u: AccessCredentials[dto.User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstname *: u.data.lastname *: u.data.middleName *:
          u.data.personalNumber *: u.data.phone *: u.data.roleId *: u.data.assetId *:
          u.data.branchCode *: u.data.drivingLicenseNumber *: u.data.drivingLicenseCategories *:
          u.password *: u.data.machineOperatorLicenseNumber *:
          u.data.machineOperatorLicenseCategories *: EmptyTuple
      }

  val update: Command[dto.User] =
    sql"""UPDATE users
       SET firstname = $nes,
       lastname = $nes,
       middle_name = ${nes.opt},
       personal_number = $nonNegInt,
       phone = $phone,
       role_id = ${RolesSql.id},
       asset_id = ${AssetsSql.id.opt},
       branch_code = ${nes.opt},
       driving_license_number = ${nes.opt},
       driving_license_categories = ${drivingLicenseCategories.opt},
       machine_operator_license_number = ${nes.opt},
       machine_operator_license_category = ${machineOperatorLicenseCategory.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case user: dto.User =>
          user.firstname *: user.lastname *: user.middleName *: user.personalNumber *: user.phone *: user.roleId *:
            user.assetId *: user.branchCode *: user.drivingLicenseNumber *: user.drivingLicenseCategories *:
            user.machineOperatorLicenseNumber *: user.machineOperatorLicenseCategories *:
            user.id *: EmptyTuple
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
              u.personal_number AS personal_number,
              u.phone AS phone,
              u.role_id AS role_id,
              u.asset_id AS asset_id,
              u.branch_code AS branch_code,
              u.driving_license_number,
              u.driving_license_categories,
              u.machine_operator_license_number,
              u.machine_operator_license_category,
              COUNT(*) OVER() AS total
            FROM users u"""
    baseQuery(Void).whereAndOpt(searchFilter(filters)) |+| orderBy(filters)(Void)
  }

  val delete: Command[UserId] =
    sql"""DELETE FROM users WHERE id = $id""".command

  val updatePassword: Command[PasswordHash[SCrypt] *: UserId *: EmptyTuple] =
    sql"""UPDATE users SET password = $passwordHash WHERE id = $id""".command
}
