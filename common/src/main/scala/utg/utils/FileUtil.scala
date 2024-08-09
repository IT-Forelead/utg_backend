package utg.utils

import java.io.{InputStream, InputStreamReader}
import scala.jdk.CollectionConverters._
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.DefaultCSVFormat
import org.apache.poi.EncryptedDocumentException
import org.apache.poi.ss.usermodel._

object FileUtil {

  def parseCsvOrXlsInputStream(
      is: InputStream,
      fileName: String,
      delimeter: Char = ',',
      encoding: String = "ISO-8859-1",
    ): Either[String, List[List[String]]] = {
    val isCsvFile = fileName.endsWith(".csv")
    if (isCsvFile)
      parseCsvFileFromReader(fileName, is, delimeter, encoding)
    else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))
      parseSpreadsheetIs(is, fileName)
    else
      Left("Wrong file uploaded. Please upload XLSX, XLS or CSV file.")
  }

  def parseCsvFileFromReader(
      filename: String,
      is: InputStream,
      delimeter: Char = '|',
      encoding: String = "ISO-8859-1",
    ): Either[String, List[List[String]]] = {
    implicit object csvFormat extends DefaultCSVFormat {
      override val delimiter: Char = delimeter
    }
    try {
      val reader = new InputStreamReader(is, encoding)
      val matrix = CSVReader.open(reader).all()
      Right(matrix)
    }
    catch {
      case error: Throwable =>
        Left("Error occurred. Please check uploaded file.")
    }
  }

  def parseSpreadsheetIs(fis: InputStream, filename: String): Either[String, List[List[String]]] =
    try {
      val wb = WorkbookFactory.create(fis)
      var matrix: List[List[String]] = Nil
      for (i <- 0 until wb.getNumberOfSheets) {
        val sheet = wb.getSheetAt(i)
        for (row: Row <- sheet.rowIterator().asScala) {
          var rowValues = List[String]()
          for (cell: Cell <- row.cellIterator().asScala) {
            val cellValue = cell.toString.trim
            rowValues = rowValues :+ cellValue
          }
          if (rowValues.nonEmpty)
            matrix = matrix :+ rowValues
        }
      }
      Right(matrix)
    }
    catch {
      case encrErr: EncryptedDocumentException =>
        Left(
          "Error occurred. You've uploaded password protected file. Please remove password protection before uploading the file"
        )

      case error: Throwable =>
        Left("Error occurred. Please check uploaded file.")
    }
}
