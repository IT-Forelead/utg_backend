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

import utg.algebras.LineDelaysAlgebra
import utg.domain.AuthedUser
import utg.domain.LineDelayId
import utg.domain.args.lineDelays.LineDelayFilters
import utg.domain.args.lineDelays.LineDelayInput
import utg.domain.args.lineDelays.UpdateLineDelayInput
import utg.domain.enums.Privilege

final case class LineDelaysRoutes[F[_]: JsonDecoder: MonadThrow: Async](
    lineDelays: LineDelaysAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/line-delays"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[LineDelayInput] { create =>
        lineDelays.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(lineDelayId) as user if user.access(Privilege.ViewUsers) =>
      lineDelays.findById(lineDelayId.coerce[LineDelayId]).flatMap(Ok(_))

    case ar @ POST -> Root as user if user.access(Privilege.ViewUsers) =>
      ar.req.decodeR[LineDelayFilters] { create =>
        lineDelays.get(create).flatMap(Ok(_))
      }

    case DELETE -> Root / UUIDVar(lineDelayId) as user if user.access(Privilege.CreateUser) =>
      lineDelays.delete(lineDelayId.coerce[LineDelayId]).flatMap(Ok(_))

    case ar @ PUT -> Root as user if user.access(Privilege.UpdateUser) =>
      ar.req.decodeR[UpdateLineDelayInput] { update =>
        lineDelays.update(update.lineDelayId, update).flatMap(Ok(_))
      }

  }
}
