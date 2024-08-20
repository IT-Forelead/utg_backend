package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripRouteDelaysAlgebra
import utg.domain.AuthedUser
import utg.domain.TripId
import utg.domain.args.tripRouteDelays.TripRouteDelayInput

final case class TripRouteDelaysRoutes[F[_]: JsonDecoder: MonadThrow](
    tripRouteDelaysAlgebra: TripRouteDelaysAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/route-delays"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[TripRouteDelayInput] { create =>
        tripRouteDelaysAlgebra.create(create, user.id).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      tripRouteDelaysAlgebra.getByTripId(id.coerce[TripId]).flatMap(Ok(_))
  }
}
