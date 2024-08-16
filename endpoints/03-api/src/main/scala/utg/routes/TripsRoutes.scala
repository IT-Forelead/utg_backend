package utg.routes

import java.io.ByteArrayOutputStream

import scala.util.Random

import cats.MonadThrow
import cats.implicits.catsSyntaxFlatMapOps
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import org.http4s.AuthedRoutes
import org.http4s.Charset
import org.http4s.Header
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.circe.JsonDecoder
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIStringSyntax
import org.xhtmlrenderer.pdf.ITextRenderer
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripsAlgebra
import utg.domain.AuthedUser
import utg.domain.args.trips._

final case class TripsRoutes[F[_]: JsonDecoder: MonadThrow](
    tripsAlgebra: TripsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/trips"

  private val content =
    """
      |<!DOCTYPE html>
      |<html>
      |<head>
      |<style>
      |table {
      |  border-collapse: collapse;
      |  width: 100%;
      |}
      |
      |th, td {
      |  text-align: left;
      |  padding: 8px;
      |  border: 1px solid black;
      |}
      |
      |th {
      |  background-color: #f2f2f2;
      |}
      |@page { size: A4 landscape;}
      |</style>
      |</head>
      |<body>
      |<h1>Topshiriqni Bajarish izchiligi</h1>
      |<table>
      |  <tr>
      |    <th>Raqam</th>
      |    <th>Holat</th>
      |    <th>Malumot</th>
      |    <th>Vaqti</th>
      |  </tr>
      |</table>
      |</body>
      |</html>
      |""".stripMargin

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[TripInput] { create =>
        tripsAlgebra.create(create, user.branch.id).flatMap(Created(_))
      }

    case ar @ POST -> Root / "edit" as _ =>
      ar.req.decodeR[UpdateTripInput] { input =>
        tripsAlgebra.update(input) >> NoContent()
      }

    case ar @ POST -> Root / "doctor-approval" as user =>
      ar.req.decodeR[TripDoctorApprovalInput] { create =>
        tripsAlgebra.updateDoctorApproval(
          create.copy(doctorId = Some(user.id))
        ) >> NoContent()
      }

    case ar @ POST -> Root / "chief-mechanic-approval" as user =>
      ar.req.decodeR[TripChiefMechanicInput] { create =>
        tripsAlgebra.updateChiefMechanicApproval(
          create.copy(chiefMechanicId = Some(user.id))
        ) >> NoContent()
      }

    case ar @ POST -> Root as user =>
      ar.req.decodeR[TripFilters] { filters =>
        tripsAlgebra.get(filters, user.branch).flatMap(Ok(_))
      }
  }

  private def html2Pdf(htmlContent: String): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val renderer = new ITextRenderer()
    renderer.setDocumentFromString(new String(htmlContent.getBytes("UTF-8")))
    renderer.layout()
    renderer.createPDF(baos)
    baos.toByteArray
  }

  override val public: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "generate-pdf" =>
      val pdfBytes = html2Pdf(content)
      val fileName = s"trips-${Random.alphanumeric.take(10).mkString}.pdf"
      Ok(fs2.Stream.emits[F, Byte](pdfBytes))
        .map(
          _.withContentType(`Content-Type`(MediaType.application.pdf, Charset.`UTF-8`))
            .putHeaders(Header.Raw(ci"Content-Disposition", s"attachment; filename=$fileName"))
        )
  }
}
