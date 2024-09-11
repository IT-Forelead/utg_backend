package utg.auth.impl

import scala.concurrent.duration.DurationInt

import cats.data.EitherT
import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Request
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtAlgorithm
import tsec.passwordhashers.jca.SCrypt
import uz.scala.redis.RedisClient
import uz.scala.syntax.all.circeSyntaxDecoderOps
import uz.scala.syntax.refined._

import utg.Phone
import utg.algebras.SmsMessagesAlgebra
import utg.algebras.UsersAlgebra
import utg.auth.AuthConfig
import utg.auth.utils.AuthMiddleware
import utg.auth.utils.JwtExpire
import utg.auth.utils.Tokens
import utg.domain.AuthedUser
import utg.domain.args.smsMessages.SmsMessageInput
import utg.domain.args.users.DataTimeAndCount
import utg.domain.args.users.LinkCodeAndPassword
import utg.domain.auth._
import utg.domain.enums.DeliveryStatus
import utg.effects.Calendar
import utg.exception.AError
import utg.exception.AError.AuthError
import utg.exception.AError.AuthError._
import utg.utils.RandomGenerator

trait Auth[F[_], A] {
  def login(credentials: Credentials): F[AuthTokens]
  def destroySession(request: Request[F], phone: Phone): F[Unit]
  def refresh(request: Request[F]): F[AuthTokens]
  def resetPassword(phone: Phone): F[Unit]
  def validateLinkCode(linkCode: String): F[String]
  def updatePasswordWithLinkCode(input: LinkCodeAndPassword): F[Unit]
}

object Auth {
  def make[F[_]: Sync](
      config: AuthConfig,
      redis: RedisClient[F],
      users: UsersAlgebra[F],
      smsMessages: SmsMessagesAlgebra[F],
    )(implicit
      logger: Logger[F]
    ): Auth[F, AuthedUser] =
    new Auth[F, AuthedUser] {
      val tokens: Tokens[F] =
        Tokens.make[F](JwtExpire[F], config)
      val jwtAuth: JwtSymmetricAuth = JwtAuth.hmac(config.tokenKey.secret, JwtAlgorithm.HS256)

      override def login(credentials: Credentials): F[AuthTokens] =
        users.findUser(credentials.phone).flatMap {
          case None =>
            NoSuchUser("User Not Found").raiseError[F, AuthTokens]
          case Some(person) if !SCrypt.checkpwUnsafe(credentials.password, person.password) =>
            PasswordDoesNotMatch("Password does not match").raiseError[F, AuthTokens]
          case Some(person) =>
            OptionT(redis.get(credentials.phone))
              .cataF(
                createNewToken(person.data),
                json =>
                  for {
                    tokens <- json.decodeAsF[F, AuthTokens]
                    validTokens <- EitherT(
                      AuthMiddleware
                        .validateJwtToken[F](
                          JwtToken(tokens.accessToken),
                          jwtAuth,
                          _ => redis.del(tokens.accessToken, tokens.refreshToken, credentials.phone),
                        )
                    ).foldF(
                      error =>
                        logger.info(s"Tokens recreated reason of that: $error") *>
                          createNewToken(person.data),
                      _ => tokens.pure[F],
                    )
                  } yield validTokens,
              )
        }

      override def refresh(request: Request[F]): F[AuthTokens] = {
        val task = for {
          refreshToken <- EitherT(
            AuthMiddleware
              .getAndValidateJwtToken[F](
                jwtAuth,
                token =>
                  for {
                    _ <- OptionT(redis.get(AuthMiddleware.REFRESH_TOKEN_PREFIX + token))
                      .semiflatMap(_.decodeAsF[F, AuthedUser])
                      .semiflatMap(user => redis.del(user.phone))
                      .value
                    _ <- redis.del(AuthMiddleware.REFRESH_TOKEN_PREFIX + token.value)
                  } yield {},
              )
              .apply(request)
          )
          user <- EitherT
            .fromOptionF(
              redis.get(AuthMiddleware.REFRESH_TOKEN_PREFIX + refreshToken.value),
              "Refresh token expired",
            )
            .semiflatMap(_.decodeAsF[F, AuthedUser])
          _ <- EitherT.right[String](clearOldTokens(user.phone))
          tokens <- EitherT.right[String](createNewToken(user))
        } yield tokens
        task.leftMap(AuthError.InvalidToken.apply).rethrowT
      }

      override def destroySession(request: Request[F], phone: Phone): F[Unit] =
        for {
          _ <- clearOldTokens(phone)
          _ <- AuthHeaders
            .getBearerToken(request)
            .traverse_(token => redis.del(AuthMiddleware.ACCESS_TOKEN_PREFIX + token.value, phone))
        } yield {}

      override def resetPassword(phone: Phone): F[Unit] =
        users.findUser(phone).flatMap {
          case None =>
            NoSuchUser("User Not Found").raiseError[F, Unit]
          case Some(user) =>
            OptionT(redis.get(user.data.phone + "sms-count"))
              .map(_.decodeAs[DataTimeAndCount])
              .cataF(
                sentSmsLink(user.data, 0),
                data =>
                  Calendar[F].currentDateTime.flatMap { now =>
                    if (data.total <= 3 && now.isAfter(data.datetime.plusMinutes(10)))
                      sentSmsLink(user.data, data.total)
                    else
                      TryManyTimes("Try many times").raiseError[F, Unit]
                  },
              )
        }

      override def validateLinkCode(linkCode: String): F[String] =
        OptionT(redis.get(linkCode))
          .cataF(
            AError.Internal(s"Invalid code [$linkCode]").raiseError[F, String],
            _ => "Valid code!".pure[F],
          )

      override def updatePasswordWithLinkCode(input: LinkCodeAndPassword): F[Unit] =
        OptionT(redis.get(input.linkCode))
          .map(_.decodeAs[AuthedUser])
          .semiflatMap { user =>
            users.updatePassword(user.id, input.password)
          }
          .getOrElseF(AError.Internal("Invalid code").raiseError[F, Unit])

      private def createNewToken(person: AuthedUser): F[AuthTokens] =
        for {
          tokens <- tokens.createToken[AuthedUser](person)
          accessToken = AuthMiddleware.ACCESS_TOKEN_PREFIX + tokens.accessToken
          refreshToken = AuthMiddleware.REFRESH_TOKEN_PREFIX + tokens.refreshToken
          _ <- redis.put(accessToken, person, config.accessTokenExpiration.value)
          _ <- redis.put(refreshToken, person, config.refreshTokenExpiration.value)
          _ <- redis.put(person.phone, tokens, config.refreshTokenExpiration.value)
        } yield tokens

      private def clearOldTokens(phone: Phone): F[Unit] =
        OptionT(redis.get(phone))
          .semiflatMap(_.decodeAsF[F, AuthTokens])
          .semiflatMap(tokens =>
            redis.del(
              s"${AuthMiddleware.REFRESH_TOKEN_PREFIX}${tokens.refreshToken}",
              s"${AuthMiddleware.ACCESS_TOKEN_PREFIX}${tokens.accessToken}",
            )
          )
          .value
          .void

      private def sentSmsLink(user: AuthedUser, attempt: Int): F[Unit] =
        for {
          now <- Calendar[F].currentDateTime
          _ <- redis.put(
            user.phone + "sms-count",
            DataTimeAndCount(now, attempt + 1),
            1.day,
          )
          code = RandomGenerator.randomLink(6)
          _ <- redis.put(code, user, 10.minutes)
          smsText =
            s"Parolingizni qayta tiklash uchun quydagi havolaga kiring.\n http://utg.iflead.uz/reset-password/$code"
          smsMessageInput = SmsMessageInput(user.phone, smsText, DeliveryStatus.Sent)
          _ <- smsMessages.create(smsMessageInput).flatMap { message =>
            logger.info("Message is SEND:" + message)
          }
        } yield ()
    }
}
