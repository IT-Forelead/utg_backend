package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.MedicalExaminationsAlgebra
import utg.domain.AuthedUser
import utg.domain.args.medicalExaminations._

final case class MedicalExaminationsRoutes[F[_]: JsonDecoder: MonadThrow](
    medicalExaminationsAlgebra: MedicalExaminationsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/medical-examinations"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[MedicalExaminationInput] { create =>
        medicalExaminationsAlgebra.create(create, user.id).flatMap(Created(_))
      }

    case ar @ POST -> Root as _ =>
      ar.req.decodeR[MedicalExaminationFilters] { filters =>
        medicalExaminationsAlgebra.get(filters).flatMap(Ok(_))
      }
  }
}
