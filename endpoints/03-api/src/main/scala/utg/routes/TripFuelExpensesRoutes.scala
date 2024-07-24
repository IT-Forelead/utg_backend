package utg.routes

import cats.MonadThrow
import cats.implicits.catsSyntaxFlatMapOps
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
import utg.domain.args.tripFuelExpenses._
import utg.domain.enums.Privilege

final case class TripFuelExpensesRoutes[F[_]: JsonDecoder: MonadThrow](
    tripFuelExpensesAlgebra: TripFuelExpensesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/fuel-expenses"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[TripFuelExpenseInput] { create =>
        tripFuelExpensesAlgebra.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(id) as user if user.access(Privilege.ViewUsers) =>
      tripFuelExpensesAlgebra.getByTripId(id.coerce[TripId]).flatMap(Ok(_))

    case ar @ POST -> Root / "update" as user if user.access(Privilege.UpdateUser) =>
      ar.req.decodeR[UpdateTripFuelExpenseInput] { update =>
        tripFuelExpensesAlgebra.update(update) >> NoContent()
      }
  }
}
