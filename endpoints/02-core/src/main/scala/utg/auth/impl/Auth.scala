package utg.auth.impl

import cats.data.EitherT
import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import dev.profunktor.auth.jwt.JwtToken
import eu.timepit.refined.types.string.NonEmptyString
import pdi.jwt.JwtAlgorithm
import tsec.passwordhashers.jca.SCrypt
import uz.scala.redis.RedisClient
import uz.scala.syntax.all.circeSyntaxDecoderOps
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV

import utg.auth.AuthConfig
import utg.auth.utils.AuthMiddleware
import utg.auth.utils.JwtExpire
import utg.auth.utils.Tokens
import utg.domain.AuthedUser
import utg.domain.auth.AccessCredentials
import utg.domain.auth.AuthTokens
import utg.domain.auth.Credentials
import utg.exception.AError.AuthError
import utg.exception.AError.AuthError.NoSuchUser
import utg.exception.AError.AuthError.PasswordDoesNotMatch

trait Auth[F[_], A] {
  def login(credentials: Credentials): F[AuthTokens]
  def destroySession(login: NonEmptyString): F[Unit]
  def refresh(token: String): F[AuthTokens]
}

object Auth {
  def make[F[_]: Sync](
      config: AuthConfig,
      findUser: NonEmptyString => F[Option[AccessCredentials[AuthedUser]]],
      redis: RedisClient[F],
    ): Auth[F, Option[AuthedUser]] =
    new Auth[F, Option[AuthedUser]] {
      val tokens: Tokens[F] =
        Tokens.make[F](JwtExpire[F], config)
      val jwtAuth: JwtSymmetricAuth = JwtAuth.hmac(config.tokenKey.secret, JwtAlgorithm.HS256)

      override def login(credentials: Credentials): F[AuthTokens] =
        findUser(credentials.login).flatMap {
          case None =>
            NoSuchUser("User Not Found").raiseError[F, AuthTokens]
          case Some(person) if !SCrypt.checkpwUnsafe(credentials.password, person.password) =>
            PasswordDoesNotMatch("Password does not match").raiseError[F, AuthTokens]
          case Some(person) =>
            OptionT(redis.get(credentials.login))
              .cataF(
                createNewToken(person.data),
                json =>
                  for {
                    tokens <- json.decodeAsF[F, AuthTokens]
                    validTokens <- OptionT(
                      AuthMiddleware
                        .validateJwtToken[F](
                          JwtToken(tokens.accessToken),
                          jwtAuth,
                          _ => redis.del(tokens.accessToken, tokens.refreshToken, credentials.login),
                        )
                    ).cataF(
                      createNewToken(person.data),
                      _ => tokens.pure[F],
                    )
                  } yield validTokens,
              )
        }

      override def refresh(token: String): F[AuthTokens] = {
        val task = for {
          refreshToken <- EitherT.fromOptionF(
            AuthMiddleware
              .validateJwtToken[F](
                JwtToken(token),
                jwtAuth,
                jwtToken =>
                  for {
                    _ <- OptionT(redis.get(AuthMiddleware.REFRESH_TOKEN_PREFIX + jwtToken.value))
                      .semiflatMap(_.decodeAsF[F, AuthedUser])
                      .semiflatMap(person => redis.del(person.login))
                      .value
                    _ <- redis.del(AuthMiddleware.REFRESH_TOKEN_PREFIX + jwtToken.value)
                  } yield {},
              ),
            "Invalid Token",
          )
          person <- EitherT
            .fromOptionF(
              redis.get(AuthMiddleware.REFRESH_TOKEN_PREFIX + refreshToken.value),
              "Refresh token expired",
            )
            .semiflatMap(_.decodeAsF[F, AuthedUser])
          _ <- EitherT.right[String](clearOldTokens(person.login))
          tokens <- EitherT.right[String](createNewToken(person))
        } yield tokens
        task.leftMap(AuthError.InvalidToken.apply).rethrowT
      }

      override def destroySession(login: NonEmptyString): F[Unit] =
        for {
          _ <- clearOldTokens(login)
          _ <- redis.del(login)
        } yield {}

      private def createNewToken(person: AuthedUser): F[AuthTokens] =
        for {
          tokens <- tokens.createToken[AuthedUser](person)
          accessToken = AuthMiddleware.ACCESS_TOKEN_PREFIX + tokens.accessToken
          refreshToken = AuthMiddleware.REFRESH_TOKEN_PREFIX + tokens.refreshToken
          _ <- redis.put(accessToken, person, config.accessTokenExpiration.value)
          _ <- redis.put(refreshToken, person, config.refreshTokenExpiration.value)
          _ <- redis.put(person.login, tokens, config.refreshTokenExpiration.value)
        } yield tokens

      private def clearOldTokens(login: NonEmptyString): F[Unit] =
        OptionT(redis.get(login))
          .semiflatMap(_.decodeAsF[F, AuthTokens])
          .semiflatMap(tokens =>
            redis.del(
              s"${AuthMiddleware.REFRESH_TOKEN_PREFIX}${tokens.refreshToken}",
              s"${AuthMiddleware.ACCESS_TOKEN_PREFIX}${tokens.accessToken}",
            )
          )
          .value
          .void
    }
}
