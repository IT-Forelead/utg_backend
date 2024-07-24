package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripsAlgebra
import utg.domain.AuthedUser
import utg.domain.args.trips.TripFilters
import utg.domain.args.trips.TripInput

final case class TripsRoutes[F[_]: JsonDecoder: MonadThrow](
    tripsAlgebra: TripsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/trips"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" =>
      ar.req.decodeR[TripInput] { create =>
        tripsAlgebra.create(create).flatMap(Created(_))
      }

    case ar @ POST -> Root =>
      ar.req.decodeR[TripFilters] { filters =>
        tripsAlgebra.get(filters).flatMap(Ok(_))
      }
  }
}
