package utg.routes

import cats.MonadThrow
import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.{AuthedRoutes, Charset, Headers, MediaType, Response}
import org.http4s.circe.JsonDecoder
import org.http4s.headers.{`Content-Disposition`, `Content-Type`}
import org.typelevel.ci.CIStringSyntax
import utg.algebras.TripDriverTasksAlgebra
import utg.domain.args.tripDriverTasks.{TripDriverTaskFilters, TripDriverTaskInput, UpdateTripDriverTaskInput}
import utg.domain.{AuthedUser, TripDriverTaskId}
import utg.domain.enums.Privilege
import uz.scala.http4s.syntax.all.{deriveEntityEncoder, http4SyntaxReqOps}
import uz.scala.http4s.utils.Routes
import utg.domain.TripDriverTaskCsvGenerator.makeCsv

final case class TripDriverTasksRoutes[F[_]: JsonDecoder: MonadThrow: Async](
                                                                              tripDriverTasks: TripDriverTasksAlgebra[F],
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
    case ar @ POST -> Root / "create" as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[TripDriverTaskInput] { create =>
        tripDriverTasks.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(tripDriverTaskId) as user if user.access(Privilege.ViewUsers) =>
      tripDriverTasks.findById(tripDriverTaskId.coerce[TripDriverTaskId]).flatMap(Ok(_))

    case ar @ POST -> Root as user if user.access(Privilege.ViewUsers) =>
      ar.req.decodeR[TripDriverTaskFilters] { create =>
        tripDriverTasks.get(create).flatMap(Ok(_))
      }

    case GET -> Root / "csv" as user if user.access(Privilege.ViewUsers) =>
      tripDriverTasks
        .getAsStream(TripDriverTaskFilters())
        .map { report =>
          csvResponse(
            report.through(makeCsv[F]),
            "Trip_Driver_Tasks_Report.csv",
          )
        }

    case DELETE -> Root / UUIDVar(tripDriverTaskId) as user if user.access(Privilege.CreateUser) =>
      tripDriverTasks.delete(tripDriverTaskId.coerce[TripDriverTaskId]).flatMap(Ok(_))

    case ar @ PUT -> Root as user if user.access(Privilege.UpdateUser) =>
      ar.req.decodeR[UpdateTripDriverTaskInput] { update =>
        tripDriverTasks.update(update.tripDriverTaskId, update).flatMap(Ok(_))
      }
  }
}
