package utg.auth.utils

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import dev.profunktor.auth.jwt._
import org.http4s.Credentials.Token
import org.http4s._
import org.http4s.headers.Authorization
import org.http4s.server
import pdi.jwt._

import utg.exception.AError

object AuthMiddleware {
  val ACCESS_TOKEN_PREFIX = "ACCESS_"
  val REFRESH_TOKEN_PREFIX = "REFRESH_"
  private def getBearerToken[F[_]: Sync]: Kleisli[F, Request[F], Option[JwtToken]] =
    Kleisli { request =>
      Sync[F].delay(
        request
          .headers
          .get[Authorization]
          .collect {
            case Authorization(Token(AuthScheme.Bearer, token)) => JwtToken(token)
          }
          .orElse {
            request.params.get("X-Access-Token").map(JwtToken.apply)
          }
      )
    }

  def validateJwtToken[F[_]: Sync](
      token: JwtToken,
      jwtAuth: JwtSymmetricAuth,
      removeToken: JwtToken => F[Unit],
    ): F[Option[JwtToken]] =
    Jwt
      .decode(
        token.value,
        jwtAuth.secretKey.value,
        jwtAuth.jwtAlgorithms,
      )
      .liftTo
      .map(_ => token.some)
      .handleErrorWith { error =>
        removeToken(token).flatMap { _ =>
          AError.AuthError.InvalidToken(error.getMessage).raiseError[F, Option[JwtToken]]
        }
      }

  def getAndValidateJwtToken[F[_]: Sync](
      jwtAuth: JwtSymmetricAuth,
      removeToken: JwtToken => F[Unit],
    ): Kleisli[F, Request[F], Option[JwtToken]] =
    Kleisli { request =>
      OptionT(getBearerToken[F].apply(request)).flatMapF { token =>
        validateJwtToken(token, jwtAuth, removeToken)
      }.value
    }

  def apply[F[_]: Sync, A](
      jwtAuth: JwtSymmetricAuth,
      authenticate: String => F[Option[A]],
      removeToken: JwtToken => F[Unit],
    ): server.AuthMiddleware[F, Option[A]] = { routes: AuthedRoutes[Option[A], F] =>
    def getUser(
        token: JwtToken
      ): F[Option[A]] =
      authenticate(ACCESS_TOKEN_PREFIX + token.value)

    Kleisli { (req: Request[F]) =>
      OptionT {
        OptionT(getAndValidateJwtToken[F](jwtAuth, removeToken).run(req))
          .flatMapF(getUser)
          .value
          .flatMap(user => routes(AuthedRequest(user, req)).value)
      }
    }
  }
}
