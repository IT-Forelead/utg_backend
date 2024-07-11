package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import utg.algebras.RolesAlgebra
import utg.algebras.UsersAlgebra
import utg.domain.AuthedUser
import utg.domain.UserId
import utg.domain.args.users.{UpdateUserInput, UserFilters, UserInput}
import utg.domain.enums.Privilege

final case class UsersRoutes[F[_]: JsonDecoder: MonadThrow](
    users: UsersAlgebra[F],
    roles: RolesAlgebra[F],
  ) extends Routes[F, AuthedUser] {
  override val path = "/users"

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
