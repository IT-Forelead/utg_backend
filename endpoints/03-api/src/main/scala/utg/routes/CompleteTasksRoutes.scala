package utg.routes

import cats.MonadThrow
import cats.effect.Async
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.CompleteTasksAlgebra
import utg.domain.AuthedUser
import utg.domain.CompleteTaskId
import utg.domain.args.completeTasks.CompleteTaskFilters
import utg.domain.args.completeTasks.CompleteTaskInput
import utg.domain.args.completeTasks.UpdateCompleteTaskInput
import utg.domain.enums.Privilege

final case class CompleteTasksRoutes[F[_]: JsonDecoder: MonadThrow: Async](
    completeTasks: CompleteTasksAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/complete-tasks"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[CompleteTaskInput] { create =>
        completeTasks.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(completeTaskId) as user if user.access(Privilege.ViewUsers) =>
      completeTasks.findById(completeTaskId.coerce[CompleteTaskId]).flatMap(Ok(_))

    case ar @ POST -> Root as user if user.access(Privilege.ViewUsers) =>
      ar.req.decodeR[CompleteTaskFilters] { create =>
        completeTasks.get(create).flatMap(Ok(_))
      }

    case DELETE -> Root / UUIDVar(completeTaskId) as user if user.access(Privilege.CreateUser) =>
      completeTasks.delete(completeTaskId.coerce[CompleteTaskId]).flatMap(Ok(_))

    case ar @ PUT -> Root as user if user.access(Privilege.UpdateUser) =>
      ar.req.decodeR[UpdateCompleteTaskInput] { update =>
        completeTasks.update(update.completeTaskId, update).flatMap(Ok(_))
      }

  }
}
