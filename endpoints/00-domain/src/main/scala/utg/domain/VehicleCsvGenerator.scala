package utg.domain

import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import cats.effect.Concurrent
import cats.effect.Sync
import com.github.tototoshi.csv.CSVWriter
import fs2.text.utf8

object VehicleCsvGenerator {
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
      "Branch",
      "Vehicle Category",
      "Brand",
      "Registered Number",
      "Inventory Number",
      "Year Of Release",
      "Body Number",
      "Chassis Number",
      "Engine Number",
      "Condition Type",
      "Fuel Types",
      "Description",
      "Gps Tracking",
      "Fuel Level Sensor",
      "Fuel Tank Volume",
    )
  private def toCSVField(vehicle: Vehicle): List[String] =
    List[String](
      ldtToString(vehicle.createdAt),
      vehicle.vehicleType.entryName,
      vehicle.branch.map(_.name.value).getOrElse(""),
      vehicle.vehicleCategory.map(_.name.value).getOrElse(""),
      vehicle.brand.value,
      vehicle.registeredNumber.map(_.value).getOrElse(""),
      vehicle.inventoryNumber.value,
      vehicle.yearOfRelease.value.toString,
      vehicle.bodyNumber.map(_.value).getOrElse(""),
      vehicle.chassisNumber.map(_.value).getOrElse(""),
      vehicle.engineNumber.map(_.value).getOrElse(""),
      vehicle.conditionType.entryName,
      vehicle.fuelTypes.map(_.toList.map(_.entryName).mkString(",")).getOrElse(""),
      vehicle.description.map(_.value).getOrElse(""),
      vehicle.gpsTracking.map(_.entryName).getOrElse(""),
      vehicle.fuelLevelSensor.map(_.value).getOrElse(0.0).toString,
      vehicle.fuelTankVolume.map(_.value).getOrElse(0.0).toString,
    )

  def makeCsv[F[_]: Concurrent: Sync]: fs2.Pipe[F, Vehicle, Byte] =
    report =>
      fs2
        .Stream
        .fromIterator[F](List(CsvHeaders).map(writeAsCsv).iterator, 128)
        .merge(report.map(toCSVField).map(writeAsCsv))
        .through(utf8.encode)
}
