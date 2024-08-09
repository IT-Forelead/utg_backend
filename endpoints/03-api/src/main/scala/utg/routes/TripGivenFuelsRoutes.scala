package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripGivenFuelsAlgebra
import utg.domain.AuthedUser
import utg.domain.args.tripFuelExpenses._

final case class TripGivenFuelsRoutes[F[_]: JsonDecoder: MonadThrow](
    tripGivenFuelsAlgebra: TripGivenFuelsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/given-fuels"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[TripGivenFuelInput] { create =>
        tripGivenFuelsAlgebra.create(create, user.id).flatMap(Created(_))
      }
  }
}
