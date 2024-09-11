package utg.routes

import cats.MonadThrow
import cats.implicits._
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.SmsMessagesAlgebra
import utg.domain.AuthedUser
import utg.domain.args.smsMessages.SmsMessageFilters

final case class SmsMessagesRoutes[F[_]: JsonDecoder: MonadThrow](
    smsMessagesAlgebra: SmsMessagesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/sms-messages"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[SmsMessageFilters] { filters =>
        smsMessagesAlgebra.get(filters).flatMap(Ok(_))
      }
  }
}
