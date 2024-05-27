package utg.setup

import cats.Monad
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.effect.std.Console
import cats.effect.std.Dispatcher
import cats.effect.std.Random
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import utg.domain.AuthedUser
import utg.domain.auth.AccessCredentials
import eu.timepit.refined.pureconfig._
import org.http4s.server
import org.typelevel.log4cats.Logger
import pureconfig.generic.auto.exportReader
import uz.scala.aws.s3.S3Client
import uz.scala.flyway.Migrations
import uz.scala.mailer.Mailer
import uz.scala.redis.RedisClient
import uz.scala.skunk.SkunkSession

import utg.Algebras
import utg.Repositories
import utg.auth.impl.Auth
import utg.auth.impl.LiveMiddleware
import utg.http.{ Environment => ServerEnvironment }
import utg.EmailAddress
import utg.utils.ConfigLoader

case class Environment[F[_]: Async: Logger: Dispatcher: Random](
    config: Config,
    repositories: Repositories[F],
    auth: Auth[F, Option[AuthedUser]],
    s3Client: S3Client[F],
    mailer: Mailer[F],
    middleware: server.AuthMiddleware[F, Option[AuthedUser]],
  ) {
  private val algebras: Algebras[F] = Algebras.make[F](auth, repositories, s3Client, mailer)

  lazy val toServer: ServerEnvironment[F] =
    ServerEnvironment(
      config = config.http,
      middleware = middleware,
      algebras = algebras,
    )
}
object Environment {
  private def findUser[F[_]: Monad](
      repositories: Repositories[F]
    ): EmailAddress => F[Option[AccessCredentials[AuthedUser]]] = email =>
    OptionT(repositories.users.find(email))
      .map(identity[AccessCredentials[AuthedUser]])
      .value

  def make[F[_]: Async: Console: Logger: Dispatcher]: Resource[F, Environment[F]] =
    for {
      config <- Resource.eval(ConfigLoader.load[F, Config])
      _ <- Resource.eval(Migrations.run[F](config.migrations))
      repositories <- SkunkSession.make[F](config.database).map { implicit session =>
        Repositories.make[F]
      }
      redis <- Redis[F].utf8(config.redis.uri.toString).map(RedisClient[F](_, config.redis.prefix))
      implicit0(random: Random[F]) <- Resource.eval(Random.scalaUtilRandom[F])

      middleware = LiveMiddleware.make[F](config.auth, redis)
      auth = Auth.make[F](config.auth, findUser(repositories), redis)
      s3Client <- S3Client.resource(config.awsConfig)
      mailer = Mailer.make[F](config.mailer)
    } yield Environment[F](config, repositories, auth, s3Client, mailer, middleware)
}
