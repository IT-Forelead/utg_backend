package utg.routes

import cats.MonadThrow
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripVehicleAcceptancesAlgebra
import utg.domain.AuthedUser
import utg.domain.TripId
import utg.domain.args.tripVehicleAcceptances.TripVehicleAcceptanceInput

final case class TripVehicleAcceptancesRoutes[F[_]: JsonDecoder: MonadThrow](
    tripVehicleAcceptancesAlgebra: TripVehicleAcceptancesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/vehicle-acceptances"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[TripVehicleAcceptanceInput] { create =>
        tripVehicleAcceptancesAlgebra
          .create(create.copy(mechanicId = user.id.some))
          .flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      tripVehicleAcceptancesAlgebra.getByTripId(id.coerce[TripId]).flatMap(Ok(_))
  }
}
