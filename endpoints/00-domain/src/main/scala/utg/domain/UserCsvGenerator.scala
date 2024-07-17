package utg.domain

import cats.effect.{Concurrent, Sync}
import com.github.tototoshi.csv.CSVWriter
import fs2.text.utf8
import utg.domain.AuthedUser.User

import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object UserCsvGenerator {
  private val CsvHeaders: List[String] =
    List(
      "Created Date",
      "First Name",
      "Last Name",
      "Phone",
      "Role",
      "Branch"
    )
  def writeAsCsv(rows: List[String]): String = {
    val writer = new StringWriter()
    val csvWriter = CSVWriter.open(writer)
    csvWriter.writeRow(rows)
    writer.toString
  }
  def ldtToString(date: ZonedDateTime, format: String = "dd-MM-yyyy HH:mm"): String =
    date.format(DateTimeFormatter.ofPattern(format))

  private def toCSVField(user: AuthedUser.User): List[String] = {
    List[String](
      ldtToString(user.createdAt),
      user.firstname.value,
      user.lastname.value,
      user.phone.value,
      user.role.name.value,
      user.branch.map(_.name.value).getOrElse(""),
    )
  }

  def makeCsv[F[_]: Concurrent: Sync]: fs2.Pipe[F, User, Byte] =
    report =>
      fs2
        .Stream
        .fromIterator[F](List(CsvHeaders).map(writeAsCsv).iterator, 128)
        .merge(report.map(toCSVField).map(writeAsCsv))
        .through(utf8.encode)
}
