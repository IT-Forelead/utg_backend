package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripVehicleIndicatorsAlgebra
import utg.domain.AuthedUser
import utg.domain.args.tripVehicleIndicators.TripVehicleIndicatorInput
import utg.domain.enums.Privilege

final case class TripVehicleIndicatorsRoutes[F[_]: JsonDecoder: MonadThrow](
    tripVehicleIndicatorsAlgebra: TripVehicleIndicatorsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/trips"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "vehicle" / "indicator" / "create" as user
         if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[TripVehicleIndicatorInput] { create =>
        tripVehicleIndicatorsAlgebra.create(create).flatMap(Created(_))
      }
  }
}
