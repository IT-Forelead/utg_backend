package utg.routes

import cats.MonadThrow
import cats.effect.Async
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import org.http4s.AuthedRoutes
import org.http4s.Charset
import org.http4s.Headers
import org.http4s.MediaType
import org.http4s.Response
import org.http4s.circe.JsonDecoder
import org.http4s.headers.`Content-Disposition`
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIStringSyntax
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import utg.algebras.BranchesAlgebra
import utg.domain.AuthedUser
import utg.domain.BranchCsvGenerator.makeCsv
import utg.domain.args.branches._
import utg.domain.enums.Privilege

final case class BranchesRoutes[F[_]: JsonDecoder: MonadThrow: Async](
    branchesAlgebra: BranchesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/branches"

  private def csvResponse(body: fs2.Stream[F, Byte], filename: String): Response[F] =
    Response(
      body = body,
      headers = Headers(
        `Content-Disposition`(
          "attachment",
          Map(ci"filename" -> filename),
        ),
        `Content-Type`(MediaType.text.csv, Charset.`UTF-8`),
      ),
    )

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[BranchInput] { create =>
        branchesAlgebra.create(create).flatMap(Created(_))
      }

    case GET -> Root as user if user.access(Privilege.ViewUsers) =>
      branchesAlgebra.getBranches.flatMap(Ok(_))

    case ar @ PUT -> Root as user if user.access(Privilege.CreateUser) =>
      ar.req.decodeR[UpdateBranchInput] { update =>
        branchesAlgebra.update(update).flatMap(Accepted(_))
      }

    case GET -> Root / "csv" as user if user.access(Privilege.ViewUsers) =>
      branchesAlgebra
        .getAsStream(BranchFilters())
        .map { report =>
          csvResponse(
            report.through(makeCsv[F]),
            "Branches_Report.csv",
          )
        }
  }
}
