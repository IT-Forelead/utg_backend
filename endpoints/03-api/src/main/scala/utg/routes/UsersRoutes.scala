package utg.routes

import java.io.InputStream
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.implicits._
import eu.timepit.refined.types.all.NonNegInt
import io.estatico.newtype.ops.toCoercibleIdOps
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.headers.`Content-Disposition`
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Multipart
import org.http4s.multipart.Part
import org.typelevel.ci.CIStringSyntax
import uz.scala.http4s.syntax.all._
import uz.scala.http4s.utils.Routes
import uz.scala.syntax.refined._
import utg.algebras.RolesAlgebra
import utg.algebras.UsersAlgebra
import utg.domain.AuthedUser
import utg.domain.UserId
import utg.domain.args.users._
import utg.domain.enums.DrivingLicenseCategory
import utg.domain.enums.MachineOperatorLicenseCategory
import utg.exception.AError
import utg.repos.sql.dto.User
import utg.utils.FileUtil

import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class UsersRoutes[F[_]: JsonDecoder: Async](
    users: UsersAlgebra[F],
    roles: RolesAlgebra[F],
  ) extends Routes[F, AuthedUser] {
  override val path = "/users"

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

  def parseLocalDate(dateStr: String, dateFormat: String = "yyyy-MM-dd"): LocalDate =
    LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(dateFormat))

  private def uploadUsers(part: Part[F])(is: InputStream): F[Unit] =
    for {
      roles <- roles.getAll.map(roles => roles.map(role => (role.name.value, role.id)))
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
                val drivingLicenseCategories =
                  if (row(8).trim.isEmpty)
                    List.empty[String]
                  else
                    row(8).trim.split(",").map(_.trim).toList
                val makePrettyDrivingLicense = NonEmptyList.fromList(
                  drivingLicenseCategories.map(DrivingLicenseCategory.withName)
                )
                val machineOperatorLicenseCategories =
                  if (row(19).trim.isEmpty)
                    List.empty[String]
                  else
                    row(19).trim.split(",").map(_.trim).toList
                val makePrettyMachineOperatorLicense = NonEmptyList.fromList(
                  machineOperatorLicenseCategories.map(MachineOperatorLicenseCategory.withName)
                )
                val roleIdOpt = roles.toMap.get(row.head)
                roleIdOpt.fold(
                  AError.Internal(s"Role is not valid: ${row.head}").raiseError[F, UserId]
                ) { roleId =>
                  users.create(
                    UserInput(
                      row(1),
                      row(2),
                      row.lift(3),
                      row.lift(4).map(_.toLong),
                      row.lift(5).map(parseLocalDate(_)),
                      row.lift(6),
                      row.lift(7),
                      NonNegInt.unsafeFrom(row(8).toInt),
                      row(9),
                      roleId,
                      row(12),
                      row.lift(12),
                      makePrettyDrivingLicense,
                      row.lift(15).map(parseLocalDate(_)),
                      row.lift(15).map(parseLocalDate(_)),
                      row.lift(16),
                      row.lift(17),
                      makePrettyMachineOperatorLicense,
                      row.lift(20).map(parseLocalDate(_)),
                      row.lift(21).map(parseLocalDate(_)),
                      row.lift(22),
                      None
                    )
                  )
                }
              },
        )
    } yield ()

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as _ =>
      ar.req.decodeR[UserInput] { create =>
        users.create(create).flatMap(Created(_))
      }

    case GET -> Root / UUIDVar(userId) as _ =>
      users.findById(userId.coerce[UserId]).flatMap(Ok(_))

    case ar @ POST -> Root as _ =>
      ar.req.decodeR[UserFilters] { create =>
        users.get(create).flatMap(Ok(_))
      }

    case GET -> Root / "csv" as _ =>
      users
        .getAsStream(UserFilters())
        .map { report =>
          csvResponse(
            report.through(User.makeCsv[F]),
            "Users_Report.csv",
          )
        }

    case GET -> Root / "roles" as _ =>
      roles.getAll.flatMap(Ok(_))

    case DELETE -> Root / UUIDVar(userId) as _ =>
      users.delete(userId.coerce[UserId]).flatMap(Ok(_))

    case ar @ PUT -> Root as _ =>
      ar.req.decodeR[UpdateUserInput] { update =>
        users.update(update.userId, update).flatMap(Ok(_))
      }

    case ar @ POST -> Root / "roles" as _ =>
      ar.req.decodeR[CreateRoleInput] { value =>
        roles.createRole(value.name).flatMap(Ok(_))
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
                .evalMap(uploadUsers(part))
                .compile
                .drain
            }
            .flatMap(_ => Accepted("Successfully uploaded"))
        else UnsupportedMediaType("Invalid file format. Only Xls, Xlsx and CSV files are accepted")
      }
  }
}
