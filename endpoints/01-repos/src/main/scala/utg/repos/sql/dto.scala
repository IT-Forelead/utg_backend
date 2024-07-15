package utg.repos.sql

import cats.effect.{Concurrent, Sync}
import com.github.tototoshi.csv.CSVWriter

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import fs2.text.utf8
import io.scalaland.chimney.dsl._
import uz.scala.syntax.refined._

import utg.Phone
import utg.domain
import utg.domain.AssetId
import utg.domain.AuthedUser
import utg.domain.BranchId
import utg.domain.RegionId
import utg.domain.RoleId
import utg.domain.UserId
import utg.domain.VehicleCategoryId

import java.io.StringWriter
import java.time.format.DateTimeFormatter

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
    )
  case class Branch(
      id: BranchId,
      name: NonEmptyString,
      code: NonEmptyString,
      regionId: RegionId,
      deleted: Boolean = false,
    )
  case class VehicleCategory(
      id: VehicleCategoryId,
      name: NonEmptyString,
      deleted: Boolean = false,
    )
}
