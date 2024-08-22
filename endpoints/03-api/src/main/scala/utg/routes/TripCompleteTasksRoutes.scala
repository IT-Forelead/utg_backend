package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.TripCompleteTasksAlgebra
import utg.domain.AuthedUser
import utg.domain.TripId
import utg.domain.args.tripCompleteTasks.TripCompleteTaskInput

final case class TripCompleteTasksRoutes[F[_]: JsonDecoder: MonadThrow](
    tripCompleteTasksAlgebra: TripCompleteTasksAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/complete-tasks"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[TripCompleteTaskInput] { create =>
        tripCompleteTasksAlgebra.create(create, user.id).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(id) as _ =>
      tripCompleteTasksAlgebra.getByTripId(id.coerce[TripId]).flatMap(Ok(_))
  }
}
