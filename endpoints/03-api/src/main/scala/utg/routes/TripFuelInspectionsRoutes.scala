package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripFuelInspectionsAlgebra
import utg.domain.AuthedUser
import utg.domain.TripId
import utg.domain.args.tripFuelExpenses._

final case class TripFuelInspectionsRoutes[F[_]: JsonDecoder: MonadThrow](
    tripFuelInspectionsAlgebra: TripFuelInspectionsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/fuel-inspections"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[TripFuelInspectionInput] { create =>
        tripFuelInspectionsAlgebra.create(create, user.id).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      tripFuelInspectionsAlgebra.getByTripId(id.coerce[TripId]).flatMap(Ok(_))
  }
}
