package utg.routes

import cats.MonadThrow
import cats.data.{NonEmptyList, OptionT}
import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeErrorId, toFlatMapOps, toFunctorOps, toTraverseOps}
import org.http4s.AuthedRoutes
import org.http4s.Charset
import org.http4s.Headers
import org.http4s.MediaType
import org.http4s.Response
import org.http4s.circe.JsonDecoder
import org.http4s.headers.`Content-Disposition`
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.{Multipart, Part}
import org.typelevel.ci.CIStringSyntax
import uz.scala.http4s.syntax.all.{deriveEntityEncoder, http4SyntaxPartOps, http4SyntaxReqOps}
import uz.scala.http4s.utils.Routes
import utg.algebras.{BranchesAlgebra, VehiclesAlgebra, VehicleCategoriesAlgebra}
import utg.domain.{AuthedUser, VehicleId}
import utg.domain.VehicleCsvGenerator.makeCsv
import utg.domain.args.vehicles.VehicleFilters
import utg.domain.args.vehicles.VehicleInput
import utg.exception.AError

import java.io.InputStream

final case class VehiclesRoutes[F[_]: JsonDecoder: MonadThrow: Async](
    vehiclesAlgebra: VehiclesAlgebra[F],
    branchesAlgebra: BranchesAlgebra[F],
    vehicleCategoriesAlgebra: VehicleCategoriesAlgebra[F],
  ) extends Routes[F, AuthedUser] {
  override val path = "/vehicles"

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

  private def uploadVehicles(part: Part[F])(is: InputStream): F[Unit] =
    for {
      branches <- branchesAlgebra.getBranches.map(branches => branches.map(branch => (branch.name.value, branch.id)))
      vehicleCategories <- vehicleCategoriesAlgebra.getBranches.map(branches => branches.map(branch => (branch.name.value, branch.id)))
      _ <- OptionT
        .fromOption[F](
          part
            .filename
            .flatMap(filename => FileUtil.parseCsvOrXlsInputStream(is, filename).toOption)
        )
        .cataF(
          AError.Internal("Can't parse file").raiseError[F, Unit],
          matrix =>
            matrix
              .tail
              .traverse_ { row =>
                val listCategories = if (row(8).trim.isEmpty) {
                  List.empty[String]
                } else {
                  row(8).trim.split(",").map(_.trim).toList
                }
                val makePrettyLicense = NonEmptyList.fromList(
                  listCategories.map(DrivingLicenseCategory.withName)
                )
                val branchIdOpt = branches.toMap.get(row.head)
                branchIdOpt.fold(
                  AError.Internal(s"Branch is not valid: ${row.head}").raiseError[F, VehicleId]
                ) { roleId =>
                  vehiclesAlgebra.create(
                    VehicleInput(
                      roleId,
                      vehicleCategoryId,
                      row(1),
                      row(2),
                      row.lift(3),
                      NonNegInt.unsafeFrom(row(4).toInt),
                      row(5),
                      row(6),
                      row.lift(7),
                      makePrettyLicense,
                    )
                  )
                }
              },
        )
    } yield ()

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as _ =>
      ar.req.decodeR[VehicleInput] { create =>
        vehiclesAlgebra.create(create).flatMap(Created(_))
      }

    case ar @ POST -> Root as _ =>
      ar.req.decodeR[VehicleFilters] { create =>
        vehiclesAlgebra.get(create).flatMap(Ok(_))
      }

    case GET -> Root / "csv" as _ =>
      vehiclesAlgebra
        .getAsStream(VehicleFilters())
        .map { report =>
          csvResponse(
            report.through(makeCsv[F]),
            "Vehicles_Report.csv",
          )
        }

    case ar @ POST -> Root / "document" as _ =>
      ar.req.decode[Multipart[F]] { multipart =>
        val allowedMediaTypes = List(
          MediaType.unsafeParse("text/csv"),
          MediaType.unsafeParse("application/vnd.ms-excel"),
          MediaType.unsafeParse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        )
        val filteredParts = multipart.parts.fileParts(allowedMediaTypes: _*)
        if (filteredParts.nonEmpty)
          filteredParts
            .traverse { part =>
              part
                .body
                .through(fs2.io.toInputStream)
                .evalMap(uploadVehicles(part))
                .compile
                .drain
            }
            .flatMap(_ => Accepted("Successfully uploaded"))
        else UnsupportedMediaType("Invalid file format. Only Xls, Xlsx and CSV files are accepted")
      }
  }
}
