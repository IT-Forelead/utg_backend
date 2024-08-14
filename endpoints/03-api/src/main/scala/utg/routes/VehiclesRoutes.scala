package utg.routes

import java.io.InputStream

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.toFlatMapOps
import cats.implicits.toFoldableOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import eu.timepit.refined.types.all.NonNegInt
import org.http4s.AuthedRoutes
import org.http4s.Charset
import org.http4s.Headers
import org.http4s.MediaType
import org.http4s.Response
import org.http4s.circe.JsonDecoder
import org.http4s.headers.`Content-Disposition`
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Multipart
import org.http4s.multipart.Part
import org.typelevel.ci.CIStringSyntax
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxPartOps
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import uz.scala.syntax.refined._

import utg.algebras.BranchesAlgebra
import utg.algebras.VehicleCategoriesAlgebra
import utg.algebras.VehiclesAlgebra
import utg.domain.AuthedUser
import utg.domain.VehicleCsvGenerator.makeCsv
import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters
import utg.domain.args.vehicles.VehicleInput
import utg.domain.enums.ConditionType
import utg.domain.enums.FuelType
import utg.domain.enums.GpsTrackingType
import utg.domain.enums.VehicleType
import utg.exception.AError
import utg.utils.FileUtil

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

  private val gpsMap = Map(
    "установлен" -> GpsTrackingType.Installed,
    "не установлен" -> GpsTrackingType.NotInstalled,
    "отключен" -> GpsTrackingType.Disabled,
  )

  private val conditionMap = Map(
    "исправен" -> ConditionType.Valid,
    "не исправен" -> ConditionType.Invalid,
  )

  private def uploadVehicles(part: Part[F])(is: InputStream): F[Unit] =
    for {
      branches <- branchesAlgebra
        .getBranches
        .map(branches => branches.map(branch => (branch.name.value, branch.id)))
      vehicleCategories <- vehicleCategoriesAlgebra
        .getVehicleCategories
        .map(vcList => vcList.map(vc => (vc.name.value, vc.id)))
      _ <- OptionT
        .fromOption[F](
          part
            .filename
            .flatMap(filename => FileUtil.parseCsvOrXlsInputStream(is, filename).toOption)
        )
        .cataF(
          AError.Internal("Can't parse file").raiseError[F, Unit],
          matrix => {
            matrix.filter(_.exists(_.nonEmpty))
              .tail
              .traverse_ { row =>
                val branchIdOpt = branches.toMap.get(row.head)
                val vehicleType = VehicleType.withName(row(1))
                val vehicleCategoryIdOpt = vehicleCategories.toMap.get(row(2))

                val listFuelTypes =
                  if (row(5).trim.isEmpty)
                    List.empty[String]
                  else
                    row(5).trim.split(",").map(_.trim).toList
                val makePrettyFuelType = NonEmptyList.fromList(
                  listFuelTypes.map(FuelType.withName)
                )
                val conditionType =
                  try
                    ConditionType.withName(row(8))
                  catch {
                    case _ => conditionMap(row(8).toLowerCase())
                  }

                val gpsTrackingType = row
                  .lift(13)
                  .map(gps =>
                    try
                      GpsTrackingType.withName(gps)
                    catch {
                      case _ => gpsMap(gps.toLowerCase())
                    }
                  )

                branchIdOpt.fold(
                  AError.Internal(s"Branch is not valid: ${row.head}").raiseError[F, VehicleId]
                ) { branchId =>
                  vehicleCategoryIdOpt.fold(
                    AError
                      .Internal(s"Vehicle Category is not valid: ${row(2)}")
                      .raiseError[F, VehicleId]
                  ) { vehicleCategoryId =>
                    vehiclesAlgebra.create(
                      VehicleInput(
                        branchId = branchId,
                        vehicleCategoryId = vehicleCategoryId,
                        vehicleType = vehicleType,
                        brand = row(3),
                        registeredNumber = row.lift(4),
                        inventoryNumber = row(9).replace(".0", ""),
                        yearOfRelease = NonNegInt.unsafeFrom(row(7).replace(".0", "").toInt),
                        bodyNumber = row.lift(11).map(_.replace(".0", "")),
                        chassisNumber = row.lift(12).map(_.replace(".0", "")),
                        engineNumber = row.lift(10).map(_.replace(".0", "")),
                        conditionType = conditionType,
                        fuelTypes = makePrettyFuelType,
                        description = row.lift(6),
                        gpsTracking = gpsTrackingType,
                        None,
                        None,
                      )
                    )
                  }
                }
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

    case ar @ POST -> Root / "batch-insert" as _ =>
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
