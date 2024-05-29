package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.VehiclesAlgebra
import utg.domain.AuthedUser
import utg.domain.args.vehicles.VehicleFilters
import utg.domain.args.vehicles.VehicleInput
import utg.domain.enums.Privilege

final case class VehiclesRoutes[F[_]: JsonDecoder: MonadThrow](
    vehiclesAlgebra: VehiclesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/vehicles"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[VehicleInput] { create =>
        vehiclesAlgebra.create(create).flatMap(Created(_))
      }

    case ar @ POST -> Root as user if user.access(Privilege.ViewUsers) =>
      ar.req.decodeR[VehicleFilters] { create =>
        vehiclesAlgebra.get(create).flatMap(Ok(_))
      }
  }
}
