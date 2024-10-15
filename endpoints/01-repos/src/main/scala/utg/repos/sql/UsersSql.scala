package utg.repos.sql

import shapeless.HNil
import skunk._
import skunk.codec.all.date
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
    (id *: zonedDateTime *: nes *: nes *: nes.opt *: date.opt *: nonNegInt *: phone *: RolesSql.id
      *: AssetsSql.id.opt *: nes.opt *: nes.opt *: drivingLicenseCategories.opt *: date.opt
      *: date.opt *: nes.opt *: machineOperatorLicenseCategory.opt *: date.opt *: date.opt)
      .to[dto.User]

  private val accessCredentialsDecoder: Decoder[AccessCredentials[dto.User]] =
    (codec *: passwordHash).map {
      case user *: hash *: HNil =>
        AccessCredentials(user, hash)
    }

  val findByPhone: Query[Phone, AccessCredentials[dto.User]] =
    sql"""SELECT * FROM users WHERE phone = $phone LIMIT 1""".query(accessCredentialsDecoder)

//    id UUID PRIMARY KEY NOT NULL,
//    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
//    firstname VARCHAR NOT NULL,
//    lastname VARCHAR NOT NULL,
//    middle_name VARCHAR NULL,
//    birthday DATE NULL,
//    personal_number INT NOT NULL UNIQUE,
//    phone VARCHAR NOT NULL UNIQUE,
//    role_id UUID NOT NULL CONSTRAINT fk_user_role REFERENCES roles (id) ON UPDATE CASCADE ON DELETE CASCADE,
//    asset_id UUID NULL CONSTRAINT fk_user_asset REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
//    branch_code VARCHAR NULL,
//    driving_license_number VARCHAR NULL UNIQUE,
//    driving_license_categories _DRIVING_LICENSE_CATEGORY NULL,
//    driving_license_given DATE NULL,
//    driving_license_expire DATE NULL,
//    machine_operator_license_number VARCHAR NULL UNIQUE,
//    machine_operator_license_category _MACHINE_OPERATOR_LICENSE_CATEGORY NULL,
//    machine_operator_license_given DATE NULL,
//    machine_operator_license_expire DATE NULL,
//    password VARCHAR NOT NULL

  val findById: Query[UserId, dto.User] =
    sql"""SELECT
          id,
          created_at,
          firstname,
          lastname,
          middle_name,
          birthday,
          personal_number,
          phone,
          role_id,
          asset_id,
          branch_code,
          driving_license_number,
          driving_license_categories,
          driving_license_given,
          driving_license_expire,
          machine_operator_license_number,
          machine_operator_license_category,
          machine_operator_license_given,
          machine_operator_license_expire
          FROM users
          WHERE id = $id LIMIT 1""".query(codec)

  def findByIds(ids: List[UserId]): Query[ids.type, dto.User] =
    sql"""SELECT
          id,
          created_at,
          firstname,
          lastname,
          middle_name,
          birthday,
          personal_number,
          phone,
          role_id,
          asset_id,
          branch_code,
          driving_license_number,
          driving_license_categories,
          driving_license_given,
          driving_license_expire,
          machine_operator_license_number,
          machine_operator_license_category,
          machine_operator_license_given,
          machine_operator_license_expire
          FROM users
          WHERE id IN (${id.values.list(ids)})""".query(codec)

  val insert: Command[AccessCredentials[dto.User]] =
    sql"""INSERT INTO users VALUES (
          $id,
          $zonedDateTime,
          $nes,
          $nes,
          ${nes.opt},
          ${date.opt},
          $nonNegInt,
          $phone,
          ${RolesSql.id},
          ${AssetsSql.id.opt},
          ${nes.opt},
          ${nes.opt},
          ${drivingLicenseCategories.opt},
          ${date.opt},
          ${date.opt},
          ${nes.opt},
          ${machineOperatorLicenseCategory.opt},
          ${date.opt},
          ${date.opt},
          $passwordHash
        )"""
      .command
      .contramap { (u: AccessCredentials[dto.User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstname *: u.data.lastname *: u.data.middleName *:
          u.data.birthday *: u.data.personalNumber *: u.data.phone *: u.data.roleId *:
          u.data.assetId *: u.data.branchCode *: u.data.drivingLicenseNumber *:
          u.data.drivingLicenseCategories *: u.data.drivingLicenseGiven *:
          u.data.drivingLicenseExpire *: u.data.machineOperatorLicenseNumber *:
          u.data.machineOperatorLicenseCategories *: u.data.machineOperatorLicenseGiven *:
          u.data.machineOperatorLicenseExpire *: u.password *: EmptyTuple
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
       machine_operator_license_category = ${machineOperatorLicenseCategory.opt},
       birthday = ${date.opt},
       driving_license_given = ${date.opt},
       driving_license_expire = ${date.opt},
       machine_operator_license_given = ${date.opt},
       machine_operator_license_expire = ${date.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case user: dto.User =>
          user.firstname *: user.lastname *: user.middleName *: user.personalNumber *: user.phone *: user.roleId *:
            user.assetId *: user.branchCode *: user.drivingLicenseNumber *: user.drivingLicenseCategories *:
            user.machineOperatorLicenseNumber *: user.machineOperatorLicenseCategories *: user.birthday *:
            user.drivingLicenseGiven *: user.drivingLicenseExpire *: user.machineOperatorLicenseGiven *:
            user.machineOperatorLicenseExpire *: user.id *: EmptyTuple
      }

  private def searchFilter(filters: UserFilters): List[Option[AppliedFragment]] =
    List(
      filters.id.map(sql"u.id = $id"),
      filters.roleId.map(sql"u.role_id = ${RolesSql.id}"),
      filters.name.map(s => s"%$s%").map(sql"u.firstname + ' ' + u.lastname ILIKE $varchar"),
    )

  private def orderBy(filters: UserFilters): Fragment[Void] = {
    val sorting: String = filters.sortBy.fold("created_at") {
      case UserSorting.CreatedAt => "created_at"
      case UserSorting.FirstName => "firstname"
      case UserSorting.LastName => "lastname"
      case UserSorting.Role => "role_id"
    }
    sql""" ORDER BY #$sorting #${filters.sortOrder.fold("")(_.value)}"""
  }

  def select(filters: UserFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT
            id,
            created_at,
            firstname,
            lastname,
            middle_name,
            birthday,
            personal_number,
            phone,
            role_id,
            asset_id,
            branch_code,
            driving_license_number,
            driving_license_categories,
            driving_license_given,
            driving_license_expire,
            machine_operator_license_number,
            machine_operator_license_category,
            machine_operator_license_given,
            machine_operator_license_expire
            COUNT(*) OVER() AS total
            FROM users"""
    baseQuery(Void).whereAndOpt(searchFilter(filters)) |+| orderBy(filters)(Void)
  }

  val delete: Command[UserId] =
    sql"""DELETE FROM users WHERE id = $id""".command

  val updatePassword: Command[PasswordHash[SCrypt] *: UserId *: EmptyTuple] =
    sql"""UPDATE users SET password = $passwordHash WHERE id = $id""".command
}
