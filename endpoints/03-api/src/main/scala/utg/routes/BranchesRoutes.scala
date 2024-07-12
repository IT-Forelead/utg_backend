package utg.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.BranchesAlgebra
import utg.domain.AuthedUser
import utg.domain.args.branches.BranchInput
import utg.domain.enums.Privilege

final case class BranchesRoutes[F[_]: JsonDecoder: MonadThrow](
    branchesAlgebra: BranchesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/branches"

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[BranchInput] { create =>
        branchesAlgebra.create(create).flatMap(Created(_))
      }

    case GET -> Root as user if user.access(Privilege.ViewUsers) =>
      branchesAlgebra.getBranches.flatMap(Ok(_))
  }
}
