package utg.domain

import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import cats.effect.Concurrent
import cats.effect.Sync
import com.github.tototoshi.csv.CSVWriter
import fs2.text.utf8

object VehicleHistoryCsvGenerator {
  def writeAsCsv(rows: List[String]): String = {
    val writer = new StringWriter()
    val csvWriter = CSVWriter.open(writer)
    csvWriter.writeRow(rows)
    writer.toString
  }
  def ldtToString(date: ZonedDateTime, format: String = "dd-MM-yyyy HH:mm"): String =
    date.format(DateTimeFormatter.ofPattern(format))

  private val CsvHeaders: List[String] =
    List(
      "Created Date",
      "Vehicle Type",
      "Vehicle Category",
      "Branch",
      "Registered Number",
    )
  private def toCSVField(vehicleHistory: VehicleHistory): List[String] =
    List[String](
      ldtToString(vehicleHistory.createdAt),
      vehicleHistory.vehicleCategory.vehicleType.entryName,
      vehicleHistory.vehicleCategory.name.value,
      vehicleHistory.branch.map(_.name.value).getOrElse(""),
      vehicleHistory.registeredNumber.map(_.value).getOrElse(""),
    )

  def makeCsv[F[_]: Concurrent: Sync]: fs2.Pipe[F, VehicleHistory, Byte] =
    report =>
      fs2
        .Stream
        .fromIterator[F](List(CsvHeaders).map(writeAsCsv).iterator, 128)
        .merge(report.map(toCSVField).map(writeAsCsv))
        .through(utf8.encode)
}
