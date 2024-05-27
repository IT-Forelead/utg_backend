package utg.http

import cats.effect.Async
import utg.domain.AuthedUser
import org.http4s.server
import uz.scala.http4s.HttpServerConfig

import utg.Algebras
case class Environment[F[_]: Async](
    config: HttpServerConfig,
    middleware: server.AuthMiddleware[F, Option[AuthedUser]],
    algebras: Algebras[F],
  )
