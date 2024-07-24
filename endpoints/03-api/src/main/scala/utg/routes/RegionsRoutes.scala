package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.utils.Routes

import utg.algebras.RegionsAlgebra
import utg.domain.AuthedUser

final case class RegionsRoutes[F[_]: JsonDecoder: MonadThrow](
    regionsAlgebra: RegionsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/regions"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case GET -> Root as _ =>
      regionsAlgebra.getRegions.flatMap(Ok(_))
  }
}
