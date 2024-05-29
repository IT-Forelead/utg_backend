package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.UsersAlgebra
import utg.domain.AuthedUser
import utg.domain.args.users.UserInput
import utg.domain.enums.Privilege

final case class UsersRoutes[F[_]: JsonDecoder: MonadThrow](
    users: UsersAlgebra[F]
  )(implicit
    logger: Logger[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/users"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user
         if user.role.privileges.contains(Privilege.CreateUser) =>
      ar.req.decodeR[UserInput] { create =>
        users.create(create).flatMap(Created(_))
      }
  }
}
