package utg.routes

import cats.MonadThrow
import cats.implicits.{toFlatMapOps, toFunctorOps}
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.{AuthedRoutes, Charset, Headers, MediaType, Response}
import org.http4s.circe.JsonDecoder
import org.http4s.headers.{`Content-Disposition`, `Content-Type`}
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import utg.algebras.RolesAlgebra
import utg.algebras.UsersAlgebra
import utg.domain.AuthedUser
import utg.domain.UserId
import utg.domain.args.users.{UpdateUserInput, UserFilters, UserInput}
import utg.domain.args.users.UserFilters
import utg.domain.args.users.UserInput
import utg.domain.auth.Credentials
import utg.domain.enums.Privilege
import org.typelevel.ci.CIStringSyntax
import utg.repos.sql.dto.User
import cats.effect.Async

import scala.concurrent.Future




final case class UsersRoutes[F[_]: JsonDecoder: MonadThrow: Async](
    users: UsersAlgebra[F],
    roles: RolesAlgebra[F],
  ) extends Routes[F, AuthedUser] {
  override val path = "/users"

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

  override val public: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "change-password" =>
        req.decodeR[Credentials] { credentials =>
          users.changePassword(credentials).flatMap(Ok(_))
        }
    }

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[UserInput] { create =>
        users.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(userId) as user if user.access(Privilege.ViewUsers) =>
      users.findById(userId.coerce[UserId]).flatMap(Ok(_))

    case ar @ POST -> Root as user if user.access(Privilege.ViewUsers) =>
      ar.req.decodeR[UserFilters] { create =>
        users.get(create).flatMap(Ok(_))
      }

    case ar @ POST -> Root / "csv" as user if user.access(Privilege.ViewUsers) =>
      ar.req.decodeR[UserFilters] { filter =>
        users
          .getAsStream(filter)
          .map { report =>
            csvResponse(
              report.through(User.makeCsv[F]),
              "Users_Report.csv",
            )
          }
      }

    case GET -> Root / "roles" as user if user.access(Privilege.ViewUsers) =>
      roles.getAll.flatMap(Ok(_))

    case DELETE -> Root / UUIDVar(userId) as user if user.access(Privilege.CreateUser) =>
      users.delete(userId.coerce[UserId]).flatMap(Ok(_))

    case ar @ POST -> Root / UUIDVar(userId) as user if user.access(Privilege.UpdateUser) =>
      ar.req.decodeR[UpdateUserInput] { update =>
        users.update(userId.coerce[UserId], update).flatMap(Ok(_))
      }
  }
}
