package utg.routes

import cats.MonadThrow
import cats.implicits._
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.auth.impl.Auth
import utg.domain.AuthedUser
import utg.domain.auth.Credentials

final case class AuthRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    auth: Auth[F, Option[AuthedUser]]
  ) extends Routes[F, Option[AuthedUser]] {
  override val path = "/auth"

  override val public: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        req.decodeR[Credentials] { credentials =>
          auth.login(credentials).flatMap(Ok(_))
        }

      case req @ GET -> Root / "refresh" =>
        auth.refresh(req).flatMap(Ok(_))
    }

  override val `private`: AuthedRoutes[Option[AuthedUser], F] = AuthedRoutes.of {
    case GET -> Root / "logout" as user =>
      user.traverse_(u => auth.destroySession(u.login)) *> NoContent()
  }
}
