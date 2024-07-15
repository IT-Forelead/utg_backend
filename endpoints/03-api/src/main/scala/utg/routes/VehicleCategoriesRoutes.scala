package utg.routes

import cats.MonadThrow
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.VehicleCategoriesAlgebra
import utg.domain.AuthedUser
import utg.domain.VehicleCategory
import utg.domain.enums.Privilege

final case class VehicleCategoriesRoutes[F[_]: JsonDecoder: MonadThrow](
    vehicleCategoriesAlgebra: VehicleCategoriesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/vehicle-categories"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[NonEmptyString] { name =>
        vehicleCategoriesAlgebra.create(name).flatMap(Created(_))
      }

    case GET -> Root as user if user.access(Privilege.ViewUsers) =>
      vehicleCategoriesAlgebra.get.flatMap(Ok(_))

    case ar @ PUT -> Root as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[VehicleCategory] { update =>
        vehicleCategoriesAlgebra.update(update).flatMap(Accepted(_))
      }
  }
}
