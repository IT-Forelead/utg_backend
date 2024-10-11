package utg.algebras

import java.awt.Dimension
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

import scala.io.Source

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.std.Random
import cats.implicits._
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfPage
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import org.typelevel.log4cats.Logger
import org.w3c.dom.Document
import org.xhtmlrenderer.context.StyleReference
import org.xhtmlrenderer.css.value.PageSize
import org.xhtmlrenderer.extend.TextRenderer
import org.xhtmlrenderer.pdf.ITextRenderer
import org.xhtmlrenderer.pdf.ITextTextRenderer

import utg.domain.TripFullyDetails
import utg.domain.TripId
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.utils.FileUtil.getFileAsString

trait TripFullyDetailsAlgebra[F[_]] {
  def getFullyDetails(id: TripId): F[TripFullyDetails]
  def getFullyDetailsPdf(id: TripId): F[Array[Byte]]
}

object TripFullyDetailsAlgebra {
  def make[F[_]: Calendar: GenUUID: Random](
      tripsAlgebra: TripsAlgebra[F],
      tripDriversAlgebra: TripDriversAlgebra[F],
      tripFuelSuppliesAlgebra: TripFuelSuppliesAlgebra[F],
      tripVehicleAcceptancesAlgebra: TripVehicleAcceptancesAlgebra[F],
      tripVehicleIndicatorsAlgebra: TripVehicleIndicatorsAlgebra[F],
      tripGivenFuelsAlgebra: TripGivenFuelsAlgebra[F],
      tripFuelInspectionsAlgebra: TripFuelInspectionsAlgebra[F],
      tripFuelRatesAlgebra: TripFuelRatesAlgebra[F],
      tripDriverTasksAlgebra: TripDriverTasksAlgebra[F],
      tripCompleteTasksAlgebra: TripCompleteTasksAlgebra[F],
      tripCompleteTaskAcceptancesAlgebra: TripCompleteTaskAcceptancesAlgebra[F],
      tripRouteDelaysAlgebra: TripRouteDelaysAlgebra[F],
    )(implicit
      F: MonadThrow[F],
      logger: Logger[F],
    ): TripFullyDetailsAlgebra[F] =
    new TripFullyDetailsAlgebra[F] {
      override def getFullyDetails(id: TripId): F[TripFullyDetails] =
        OptionT(tripsAlgebra.findById(id)).cataF(
          AError
            .Internal(s"Trip not found by id [$id]")
            .raiseError[F, TripFullyDetails],
          trip =>
            for {
              tripDrivers <- tripDriversAlgebra.getByTripId(id)
              tripFuelSupplies <- tripFuelSuppliesAlgebra.getByTripId(id)
              tripVehicleAcceptances <- tripVehicleAcceptancesAlgebra.getByTripId(id)
              tripVehicleIndicators <- tripVehicleIndicatorsAlgebra.getByTripId(id)
              tripGivenFuels <- tripGivenFuelsAlgebra.getByTripId(id)
              tripFuelInspections <- tripFuelInspectionsAlgebra.getByTripId(id)
              tripFuelRates <- tripFuelRatesAlgebra.getByTripId(id)
              tripDriverTasks <- tripDriverTasksAlgebra.getByTripId(id)
              tripCompleteTasks <- tripCompleteTasksAlgebra.getByTripId(id)
              tripCompleteTaskAcceptances <- tripCompleteTaskAcceptancesAlgebra.getByTripId(id)
              tripRouteDelays <- tripRouteDelaysAlgebra.getByTripId(id)
              tripFullyDetails = TripFullyDetails(
                trip = trip,
                tripDrivers = tripDrivers,
                tripFuelSupplies = tripFuelSupplies,
                tripVehicleAcceptances = tripVehicleAcceptances,
                tripVehicleIndicators = tripVehicleIndicators,
                tripGivenFuels = tripGivenFuels,
                tripFuelInspections = tripFuelInspections,
                tripFuelRates = tripFuelRates,
                tripDriverTasks = tripDriverTasks,
                tripCompleteTasks = tripCompleteTasks,
                tripCompleteTaskAcceptances = tripCompleteTaskAcceptances,
                tripRouteDelays = tripRouteDelays,
              )
            } yield tripFullyDetails,
        )

      override def getFullyDetailsPdf(id: TripId): F[Array[Byte]] =
        for {
          fullyDetails <- getFullyDetails(id)
          drivers = fullyDetails
            .tripDrivers
            .map(
              _.driver.fold("")(d => s"${d.fullName} ${d.drivingLicenseNumber.getOrElse("")}")
            )
            .mkString(", ")
          vehicle = fullyDetails
            .trip
            .vehicle
            .map(v => s"${v.brand.value} ${v.registeredNumber.getOrElse("")}")
            .getOrElse("")
          trailers = fullyDetails
            .trip
            .trailer
            .fold("")(
              _.map(t => s"${t.brand.value} ${t.registeredNumber.getOrElse("")}").mkString(", ")
            )
          semiTrailers = fullyDetails
            .trip
            .trailer
            .fold("")(
              _.map(t => s"${t.brand.value} ${t.registeredNumber.getOrElse("")}").mkString(", ")
            )
          accompanyingPersons = fullyDetails
            .trip
            .accompanyingPersons
            .fold("")(
              _.map(_.fullName).mkString(", ")
            )
          stringContent = getFileAsString("./4s-sample-form-1.html")
          updateContent = stringContent
            .replaceAll("%%START_DATE%%", fullyDetails.trip.startDate.toString)
            .replaceAll(
              "%%END_DATE%%",
              fullyDetails.trip.endDate.fold("")(str => s" - ${str.toString}"),
            )
            .replaceAll("%%SERIAL_NUMBER%%", fullyDetails.trip.serialNumber.fold("")(_.value))
            .replaceAll("%%FIRST_TAB%%", fullyDetails.trip.firstTab.fold("")(_.value))
            .replaceAll("%%SECOND_TAB%%", fullyDetails.trip.secondTab.fold("")(_.value))
            .replaceAll("%%THIRD_TAB%%", fullyDetails.trip.thirdTab.fold("")(_.value))
            .replaceAll("%%WORKING_MODE%%", fullyDetails.trip.workingMode.fold("")(_.entryName))
            .replaceAll("%%SUMMATION%%", fullyDetails.trip.summation.fold("")(_.value))
            .replaceAll("%%NOTES%%", fullyDetails.trip.notes.fold("")(_.value))
            .replaceAll("%%DRIVERS%%", drivers)
            .replaceAll("%%VEHICLE%%", vehicle)
            .replaceAll("%%TRAILERS%%", trailers)
            .replaceAll("%%SEMI_TRAILERS%%", semiTrailers)
            .replaceAll("%%ACCOMPANYING_PERSONS%%", accompanyingPersons)
          _ = println(updateContent)
          pdf = html2Pdf(updateContent)
        } yield pdf

//      createdAt: ZonedDateTime,
//      startDate: LocalDate,
//      endDate: Option[LocalDate],
//      serialNumber: Option[NonEmptyString],
//      firstTab: Option[NonEmptyString],
//      secondTab: Option[NonEmptyString],
//      thirdTab: Option[NonEmptyString],
//      workingMode: Option[WorkingModeType],
//      summation: Option[NonEmptyString],

//      vehicle: Option[Vehicle],
//      drivers: List[TripDriver],
//      trailer: Option[List[Vehicle]],
//      semiTrailer: Option[List[Vehicle]],
//      accompanyingPersons: Option[List[User]],
//      notes: Option[NonEmptyString],

      private def html2Pdf(htmlContent: String): Array[Byte] = {
        val baos = new ByteArrayOutputStream()
        val renderer = new ITextRenderer()
        renderer.setDocumentFromString(new String(htmlContent.getBytes("UTF-8")))
        renderer.getDocument
        renderer.layout()
        renderer.createPDF(baos)
        baos.toByteArray
      }
    }
}
