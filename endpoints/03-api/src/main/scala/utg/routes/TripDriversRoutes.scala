package utg.routes

import cats.MonadThrow
import cats.effect.Async
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripDriversAlgebra
import utg.domain.AuthedUser
import utg.domain.TripId
import utg.domain.args.tripDrivers.TripDriverFilters

final case class TripDriversRoutes[F[_]: JsonDecoder: MonadThrow: Async](
    tripDrivers: TripDriversAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/trip-drivers"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {

    case ar @ POST -> Root as _ =>
      ar.req.decodeR[TripDriverFilters] { filters =>
        tripDrivers.get(filters).flatMap(Ok(_))
      }

    case GET -> Root / UUIDVar(tripId) as _ =>
      tripDrivers.getByTripId(tripId.coerce[TripId]).flatMap(Ok(_))

  }
}
