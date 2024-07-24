package utg.routes

import cats.MonadThrow
import cats.effect.Async
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.Charset
import org.http4s.Headers
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.Response
import org.http4s.circe.JsonDecoder
import org.http4s.headers.`Content-Disposition`
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIStringSyntax
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.RolesAlgebra
import utg.algebras.UsersAlgebra
import utg.domain.AuthedUser
import utg.domain.UserId
import utg.domain.args.users.CreateRoleInput
import utg.domain.args.users.UpdateUserInput
import utg.domain.args.users.UserFilters
import utg.domain.args.users.UserInput
import utg.domain.auth.Credentials
import utg.repos.sql.dto.User

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
    case ar @ POST -> Root / "create" =>
      ar.req.decodeR[UserInput] { create =>
        users.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(userId) =>
      users.findById(userId.coerce[UserId]).flatMap(Ok(_))

    case ar @ POST -> Root =>
      ar.req.decodeR[UserFilters] { create =>
        users.get(create).flatMap(Ok(_))
      }

    case GET -> Root / "csv" =>
      users
        .getAsStream(UserFilters())
        .map { report =>
          csvResponse(
            report.through(User.makeCsv[F]),
            "Users_Report.csv",
          )
        }

    case GET -> Root / "roles" =>
      roles.getAll.flatMap(Ok(_))

    case DELETE -> Root / UUIDVar(userId) =>
      users.delete(userId.coerce[UserId]).flatMap(Ok(_))

    case ar @ PUT -> Root =>
      ar.req.decodeR[UpdateUserInput] { update =>
        users.update(update.userId, update).flatMap(Ok(_))
      }

    case ar @ POST -> Root / "roles" =>
      ar.req.decodeR[CreateRoleInput] { value =>
        roles.createRole(value.name).flatMap(Ok(_))
      }
  }
}
