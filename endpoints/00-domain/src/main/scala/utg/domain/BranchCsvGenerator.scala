package utg.domain

import java.io.StringWriter

import cats.effect.Concurrent
import cats.effect.Sync
import com.github.tototoshi.csv.CSVWriter
import fs2.text.utf8

object BranchCsvGenerator {
  def writeAsCsv(rows: List[String]): String = {
    val writer = new StringWriter()
    val csvWriter = CSVWriter.open(writer)
    csvWriter.writeRow(rows)
    writer.toString
  }

  private val CsvHeaders: List[String] =
    List(
      "Name",
      "Code",
      "Region",
    )
  private def toCSVField(branch: Branch): List[String] =
    List[String](
      branch.name.value,
      branch.code.value,
      branch.region.map(_.name.value).getOrElse(""),
    )

  def makeCsv[F[_]: Concurrent: Sync]: fs2.Pipe[F, Branch, Byte] =
    report =>
      fs2
        .Stream
        .fromIterator[F](List(CsvHeaders).map(writeAsCsv).iterator, 128)
        .merge(report.map(toCSVField).map(writeAsCsv))
        .through(utf8.encode)
}
