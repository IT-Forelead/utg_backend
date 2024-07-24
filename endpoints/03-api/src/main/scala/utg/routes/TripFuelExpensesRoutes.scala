package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripFuelExpensesAlgebra
import utg.domain.AuthedUser
import utg.domain.TripId
import utg.domain.args.tripFuelExpenses.TripFuelExpenseInput

final case class TripFuelExpensesRoutes[F[_]: JsonDecoder: MonadThrow](
    tripFuelExpensesAlgebra: TripFuelExpensesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/fuel-expenses"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as _ =>
      ar.req.decodeR[TripFuelExpenseInput] { create =>
        tripFuelExpensesAlgebra.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      tripFuelExpensesAlgebra.getByTripId(id.coerce[TripId]).flatMap(Ok(_))
  }
}
