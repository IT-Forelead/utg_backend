package utg.domain

import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import cats.effect.Concurrent
import cats.effect.Sync
import com.github.tototoshi.csv.CSVWriter
import fs2.text.utf8

object TripDriverTaskCsvGenerator {
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
      "Whose Discretion",
      "Arrival Time",
      "Pickup Location",
      "Delivery Location",
      "Freight Name",
      "Number Of Interactions",
      "Distance",
      "Freight Volume",
    )
  private def toCSVField(tripDriverTask: TripDriverTask): List[String] =
    List[String](
      tripDriverTask.whoseDiscretion.value,
      ldtToString(tripDriverTask.arrivalTime),
      tripDriverTask.pickupLocation.value,
      tripDriverTask.deliveryLocation.value,
      tripDriverTask.freightName.value,
      tripDriverTask.numberOfInteractions.map(_.value.toString).getOrElse(""),
      tripDriverTask.distance.map(_.value.toString).getOrElse(""),
      tripDriverTask.freightVolume.map(_.value.toString).getOrElse(""),
    )

  def makeCsv[F[_]: Concurrent: Sync]: fs2.Pipe[F, TripDriverTask, Byte] =
    report =>
      fs2
        .Stream
        .fromIterator[F](List(CsvHeaders).map(writeAsCsv).iterator, 128)
        .merge(report.map(toCSVField).map(writeAsCsv))
        .through(utf8.encode)
}
