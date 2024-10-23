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
    (id *: zonedDateTime *: nes *: nes *: nes.opt *: nonNegLong.opt *: date.opt *: nes.opt *: nes.opt *: nonNegInt
      *: phone *: RolesSql.id *: nes.opt *: nes.opt *: drivingLicenseCategories.opt *: date.opt *: date.opt *: nes.opt
      *: nes.opt *: machineOperatorLicenseCategory.opt *: date.opt *: date.opt *: nes.opt)
      .to[dto.User]

  private val accessCredentialsDecoder: Decoder[AccessCredentials[dto.User]] =
    (codec *: passwordHash).map {
      case user *: hash *: HNil =>
        AccessCredentials(user, hash)
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
          personal_id,
          birthday,
          place_of_birth,
          address,
          personal_number,
          phone,
          role_id,
          branch_code,
          driving_license_number,
          driving_license_categories,
          driving_license_given,
          driving_license_expire,
          driving_license_issuing_authority,
          machine_operator_license_number,
          machine_operator_license_category,
          machine_operator_license_given,
          machine_operator_license_expire,
          machine_operator_license_issuing_authority
          FROM users
          WHERE id = $id LIMIT 1""".query(codec)

  def findByIds(ids: List[UserId]): Query[ids.type, dto.User] =
    sql"""SELECT
          id,
          created_at,
          firstname,
          lastname,
          middle_name,
          personal_id,
          birthday,
          place_of_birth,
          address,
          personal_number,
          phone,
          role_id,
          branch_code,
          driving_license_number,
          driving_license_categories,
          driving_license_given,
          driving_license_expire,
          driving_license_issuing_authority,
          machine_operator_license_number,
          machine_operator_license_category,
          machine_operator_license_given,
          machine_operator_license_expire,
          machine_operator_license_issuing_authority
          FROM users
          WHERE id IN (${id.values.list(ids)})""".query(codec)

  val insert: Command[AccessCredentials[dto.User]] =
    sql"""INSERT INTO users VALUES (
          $id,
          $zonedDateTime,
          $nes,
          $nes,
          ${nes.opt},
          ${nonNegLong.opt},
          ${date.opt},
          ${nes.opt},
          ${nes.opt},
          $nonNegInt,
          $phone,
          ${RolesSql.id},
          ${nes.opt},
          ${nes.opt},
          ${drivingLicenseCategories.opt},
          ${date.opt},
          ${date.opt},
          ${nes.opt},
          ${nes.opt},
          ${machineOperatorLicenseCategory.opt},
          ${date.opt},
          ${date.opt},
          ${nes.opt},
          $passwordHash
        )"""
      .command
      .contramap { (u: AccessCredentials[dto.User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstname *: u.data.lastname *: u.data.middleName *:
          u.data.personalId *: u.data.birthday *: u.data.placeOfBirth *: u.data.address *:
          u.data.personalNumber *: u.data.phone *: u.data.roleId *: u.data.branchCode *:
          u.data.drivingLicenseNumber *: u.data.drivingLicenseCategories *:
          u.data.drivingLicenseGiven *: u.data.drivingLicenseExpire *:
          u.data.drivingLicenseIssuingAuthority *: u.data.machineOperatorLicenseNumber *:
          u.data.machineOperatorLicenseCategories *: u.data.machineOperatorLicenseGiven *:
          u.data.machineOperatorLicenseExpire *: u.data.machineOperatorLicenseIssuingAuthority *:
          u.password *: EmptyTuple
      }

  val update: Command[dto.User] =
    sql"""UPDATE users
       SET firstname = $nes,
       lastname = $nes,
       middle_name = ${nes.opt},
       personal_id = ${nonNegLong.opt},
       birthday = ${date.opt},
       place_of_birth = ${nes.opt},
       address = ${nes.opt},
       personal_number = $nonNegInt,
       phone = $phone,
       role_id = ${RolesSql.id},
       branch_code = ${nes.opt},
       driving_license_number = ${nes.opt},
       driving_license_categories = ${drivingLicenseCategories.opt},
       driving_license_given = ${date.opt},
       driving_license_expire = ${date.opt},
       driving_license_issuing_authority = ${nes.opt},
       machine_operator_license_number = ${nes.opt},
       machine_operator_license_category = ${machineOperatorLicenseCategory.opt},
       machine_operator_license_given = ${date.opt},
       machine_operator_license_expire = ${date.opt},
       machine_operator_license_issuing_authority = ${nes.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case user: dto.User =>
          user.firstname *: user.lastname *: user.middleName *: user.personalId *: user.birthday *:
            user.placeOfBirth *: user.address *: user.personalNumber *: user.phone *: user.roleId *:
            user.branchCode *: user.drivingLicenseNumber *: user.drivingLicenseCategories *:
            user.drivingLicenseGiven *: user.drivingLicenseExpire *: user.drivingLicenseIssuingAuthority *:
            user.machineOperatorLicenseNumber *: user.machineOperatorLicenseCategories *:
            user.machineOperatorLicenseGiven *: user.machineOperatorLicenseExpire *:
            user.machineOperatorLicenseIssuingAuthority *: user.id *: EmptyTuple
      }

  private def searchFilter(filters: UserFilters): List[Option[AppliedFragment]] =
    List(
      filters.id.map(sql"id = $id"),
      filters.roleId.map(sql"role_id = ${RolesSql.id}"),
      filters.name.map(s => s"%$s%").map(sql"firstname + ' ' + lastname ILIKE $varchar"),
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
            personal_id,
            birthday,
            place_of_birth,
            address,
            personal_number,
            phone,
            role_id,
            branch_code,
            driving_license_number,
            driving_license_categories,
            driving_license_given,
            driving_license_expire,
            driving_license_issuing_authority,
            machine_operator_license_number,
            machine_operator_license_category,
            machine_operator_license_given,
            machine_operator_license_expire,
            machine_operator_license_issuing_authority,
            COUNT(*) OVER() AS total
            FROM users"""
    baseQuery(Void).whereAndOpt(searchFilter(filters)) |+| orderBy(filters)(Void)
  }

  val delete: Command[UserId] =
    sql"""DELETE FROM users WHERE id = $id""".command

  val updatePassword: Command[PasswordHash[SCrypt] *: UserId *: EmptyTuple] =
    sql"""UPDATE users SET password = $passwordHash WHERE id = $id""".command
}
