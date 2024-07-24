package utg.routes

import cats.MonadThrow
import cats.effect.Async
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import io.estatico.newtype.ops.toCoercibleIdOps
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

import utg.algebras.TripDriverTasksAlgebra
import utg.domain.AuthedUser
import utg.domain.TripDriverTaskCsvGenerator.makeCsv
import utg.domain.TripDriverTaskId
import utg.domain.args.tripDriverTasks.TripDriverTaskFilters
import utg.domain.args.tripDriverTasks.TripDriverTaskInput
import utg.domain.args.tripDriverTasks.UpdateTripDriverTaskInput

final case class TripDriverTasksRoutes[F[_]: JsonDecoder: MonadThrow: Async](
    tripDriverTasks: TripDriverTasksAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/trip-driver-tasks"

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
    case ar @ POST -> Root / "create" =>
      ar.req.decodeR[TripDriverTaskInput] { create =>
        tripDriverTasks.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(tripDriverTaskId) =>
      tripDriverTasks.findById(tripDriverTaskId.coerce[TripDriverTaskId]).flatMap(Ok(_))

    case ar @ POST -> Root =>
      ar.req.decodeR[TripDriverTaskFilters] { create =>
        tripDriverTasks.get(create).flatMap(Ok(_))
      }

    case GET -> Root / "csv" =>
      tripDriverTasks
        .getAsStream(TripDriverTaskFilters())
        .map { report =>
          csvResponse(
            report.through(makeCsv[F]),
            "Trip_Driver_Tasks_Report.csv",
          )
        }

    case DELETE -> Root / UUIDVar(tripDriverTaskId) =>
      tripDriverTasks.delete(tripDriverTaskId.coerce[TripDriverTaskId]).flatMap(Ok(_))

    case ar @ PUT -> Root =>
      ar.req.decodeR[UpdateTripDriverTaskInput] { update =>
        tripDriverTasks.update(update.tripDriverTaskId, update).flatMap(Ok(_))
      }
  }
}
