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

import utg.algebras.TripCompleteTaskAcceptancesAlgebra
import utg.domain.AuthedUser
import utg.domain.TripId
import utg.domain.args.tripCompleteTasks.TripCompleteTaskAcceptanceInput

final case class TripCompleteTaskAcceptancesRoutes[F[_]: JsonDecoder: MonadThrow](
    tripCompleteTaskAcceptancesAlgebra: TripCompleteTaskAcceptancesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/complete-task-acceptances"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "dispatcher" / "create" as user =>
      ar.req.decodeR[TripCompleteTaskAcceptanceInput] { create =>
        tripCompleteTaskAcceptancesAlgebra
          .create(create.copy(dispatcherId = user.id.some))
          .flatMap(Created(_))
      }

    case ar @ POST -> Root / "driver" / "create" as user =>
      ar.req.decodeR[TripCompleteTaskAcceptanceInput] { create =>
        tripCompleteTaskAcceptancesAlgebra
          .create(create.copy(driverId = user.id.some))
          .flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      tripCompleteTaskAcceptancesAlgebra.getByTripId(id.coerce[TripId]).flatMap(Ok(_))
  }
}
