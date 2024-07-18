package utg.repos.sql

import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import cats.effect.Concurrent
import cats.effect.Sync
import cats.implicits.catsSyntaxOptionId
import com.github.tototoshi.csv.CSVWriter
import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import fs2.text.utf8
import io.scalaland.chimney.dsl._
import uz.scala.syntax.refined._

import utg._
import utg.domain
import utg.domain._
import utg.domain.enums._

object dto {
  case class User(
      id: UserId,
      createdAt: ZonedDateTime,
      firstname: NonEmptyString,
      lastname: NonEmptyString,
      middleName: Option[NonEmptyString],
      phone: Phone,
      roleId: RoleId,
      assetId: Option[AssetId],
      branchCode: Option[NonEmptyString],
    ) {
    def toDomain(role: domain.Role, branch: Option[utg.domain.Branch]): AuthedUser.User =
      this
        .into[AuthedUser.User]
        .withFieldConst(_.role, role)
        .withFieldConst(_.phone, phone)
        .withFieldConst(_.branch, branch)
        .transform

    def ldtToString(date: ZonedDateTime, format: String = "yyyy MM dd HH:mm"): String =
      date.format(DateTimeFormatter.ofPattern(format))

    private def toCSVField: List[String] =
      List[String](
        ldtToString(createdAt),
        firstname.value,
        lastname.value,
        phone.value,
      )
  }

  object User {
    def fromDomain(user: AuthedUser.User): User =
      user
        .into[User]
        .withFieldConst(_.roleId, user.role.id)
        .withFieldConst(_.branchCode, user.branch.map(_.code))
        .transform

    private val CsvHeaders: List[String] =
      List(
        "Created Date",
        "First Name",
        "Last Name",
        "Phone",
      )

    def makeCsv[F[_]: Concurrent: Sync]: fs2.Pipe[F, dto.User, Byte] =
      report =>
        fs2
          .Stream
          .fromIterator[F](List(CsvHeaders).map(writeAsCsv).iterator, 128)
          .merge(report.map(_.toCSVField).map(writeAsCsv))
          .through(utf8.encode)

    def writeAsCsv(rows: List[String]): String = {
      val writer = new StringWriter()
      val csvWriter = CSVWriter.open(writer)
      csvWriter.writeRow(rows)
      writer.toString
    }
  }

  case class Role(id: RoleId, name: NonEmptyString)

  case class Region(
      id: RegionId,
      name: NonEmptyString,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.Region =
      this.transformInto[domain.Region]
  }
  case class Branch(
      id: BranchId,
      name: NonEmptyString,
      code: NonEmptyString,
      regionId: RegionId,
      deleted: Boolean = false,
    ) {
    def toDomain(region: Option[domain.Region]): domain.Branch =
      this
        .into[domain.Branch]
        .withFieldConst(_.region, region)
        .transform
  }
  case class VehicleCategory(
      id: VehicleCategoryId,
      name: NonEmptyString,
      vehicleType: VehicleType,
      deleted: Boolean = false,
    )

  case class Vehicle(
      id: VehicleId,
      createdAt: ZonedDateTime,
      branchId: BranchId,
      vehicleCategoryId: VehicleCategoryId,
      vehicleType: VehicleType,
      brand: NonEmptyString,
      registeredNumber: Option[RegisteredNumber],
      inventoryNumber: InventoryNumber,
      yearOfRelease: NonNegInt,
      bodyNumber: Option[NonEmptyString],
      chassisNumber: Option[NonEmptyString],
      engineNumber: Option[NonEmptyString],
      conditionType: ConditionType,
      fuelType: Option[FuelType],
      description: Option[NonEmptyString],
      gpsTracking: Option[GpsTrackingType],
      fuelLevelSensor: Option[NonNegDouble],
      fuelTankVolume: Option[NonNegDouble],
      deleted: Boolean = false,
    ) {
    def toDomain(branch: Option[domain.Branch], vehicleCategory: domain.VehicleCategory): domain.Vehicle =
      this
        .into[domain.Vehicle]
        .withFieldConst(_.branch, branch)
        .withFieldConst(_.vehicleCategory, vehicleCategory.some)
        .transform
  }
}
