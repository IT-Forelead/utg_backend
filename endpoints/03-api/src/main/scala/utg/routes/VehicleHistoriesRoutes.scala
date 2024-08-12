package utg.routes

import cats.MonadThrow
import cats.effect.Async
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import org.http4s.AuthedRoutes
import org.http4s.Charset
import org.http4s.Headers
import org.http4s.MediaType
import org.http4s.Response
import org.http4s.circe.JsonDecoder
import org.http4s.headers.`Content-Disposition`
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIStringSyntax
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.VehicleHistoriesAlgebra
import utg.domain.AuthedUser
import utg.domain.VehicleHistoryCsvGenerator.makeCsv
import utg.domain.args.vehicleHistories.VehicleHistoryFilters

final case class VehicleHistoriesRoutes[F[_]: JsonDecoder: MonadThrow: Async](
    vehicleHistoriesAlgebra: VehicleHistoriesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/vehicle-histories"

  private def csvResponse(body: fs2.Stream[F, Byte], filename: String): Response[F] =
    Response(
      body = body,
      headers = Headers(
        `Content-Disposition`(
          "attachment",
          Map(ci"filename" -> filename),
        ),
        `Content-Type`(MediaType.text.csv, Charset.`UTF-8`),
      ),
    )

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[VehicleHistoryFilters] { create =>
        vehicleHistoriesAlgebra.get(create).flatMap(Ok(_))
      }

    case GET -> Root / "csv" as _ =>
      vehicleHistoriesAlgebra
        .getAsStream(VehicleHistoryFilters())
        .map { report =>
          csvResponse(
            report.through(makeCsv[F]),
            "Vehicle_Histories_Report.csv",
          )
        }
  }
}
