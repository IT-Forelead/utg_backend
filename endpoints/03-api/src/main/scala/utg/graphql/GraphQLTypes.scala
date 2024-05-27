package utg.graphql

import java.net.URL

import caliban.CalibanError.ExecutionError
import caliban.Value.StringValue
import caliban.schema._
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxEitherId
import eu.timepit.refined.types.all.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import uz.scala.syntax.refined._

import utg.Phone
import utg.domain.Asset
import utg.domain.AssetId
import utg.domain.AuthedUser
import utg.domain.Role
import utg.domain.RoleId
import utg.domain.UserId
import utg.domain.args.DateRange
import utg.domain.args.DateTimeRange
import utg.domain.args.users.UpdateUserRole
import utg.domain.args.users.UserFilters
import utg.domain.args.users.UserInput
import utg.domain.auth.AuthTokens
import utg.domain.auth.Credentials
import utg.domain.enums.Privilege
import utg.effects.IsUUID
import utg.graphql.args.UpdateUserInput

trait GraphQLTypes extends GenericSchema[GraphQLContext] {
  private def idArgBuilder[ID](implicit uuid: IsUUID[ID]): ArgBuilder[ID] =
    ArgBuilder.uuid.map(uuid.uuid.get)
  implicit def nelArgBuilder[A](implicit ev: ArgBuilder[A]): ArgBuilder[NonEmptyList[A]] =
    ArgBuilder.list[A].flatMap { list =>
      NonEmptyList
        .fromList(list)
        .fold {
          ExecutionError("Can't build an NonEmptyList from empty list").asLeft[NonEmptyList[A]]
        }(_.asRight[ExecutionError])
    }

  implicit val NonEmptyStringArgBuilder: ArgBuilder[NonEmptyString] =
    ArgBuilder.string.map(identity(_))
  implicit val PhoneArgBuilder: ArgBuilder[Phone] =
    ArgBuilder.string.map(identity(_))
  implicit val PosIntArgBuilder: ArgBuilder[PosInt] =
    ArgBuilder.int.map(identity(_))
  implicit val PrivilegeArgBuilder: ArgBuilder[Privilege] = ArgBuilder.gen
  implicit val RoleIdArgBuilder: ArgBuilder[RoleId] = idArgBuilder[RoleId]
  implicit val UserIdArgBuilder: ArgBuilder[UserId] = idArgBuilder[UserId]
  implicit val AssetIdArgBuilder: ArgBuilder[AssetId] = idArgBuilder[AssetId]
  implicit val UserInputArgBuilder: ArgBuilder[UserInput] = ArgBuilder.gen
  implicit val UserFiltersArgBuilder: ArgBuilder[UserFilters] = ArgBuilder.gen
  implicit val DateRangeArgBuilder: ArgBuilder[DateRange] = ArgBuilder.gen
  implicit val DateTimeRangeArgBuilder: ArgBuilder[DateTimeRange] = ArgBuilder.gen
  implicit val UpdateUserInputArgBuilder: ArgBuilder[UpdateUserInput] = ArgBuilder.gen
  implicit val UpdateUserPrivilegeArgBuilder: ArgBuilder[UpdateUserRole] = ArgBuilder.gen
  implicit val CredentialsArgBuilder: ArgBuilder[Credentials] = ArgBuilder.gen
  implicit val RoleArgBuilder: ArgBuilder[Role] = ArgBuilder.gen

  // Schemas
  private def idSchema[ID](implicit uuid: IsUUID[ID]): Schema.Typeclass[ID] =
    Schema.uuidSchema.contramap(uuid.uuid.apply)
  implicit val UnitSchema: Schema.Typeclass[Unit] =
    scalarSchema("Unit", None, None, None, _ => StringValue("OK"))
  implicit val UserIdSchema: Schema.Typeclass[UserId] = idSchema[UserId]
  implicit val RoleIdSchema: Schema.Typeclass[RoleId] = idSchema[RoleId]
  implicit val AssetIdSchema: Schema.Typeclass[AssetId] = idSchema[AssetId]
  implicit val NonEmptyStringSchema: Schema.Typeclass[NonEmptyString] =
    Schema.stringSchema.contramap[NonEmptyString](identity(_))
  implicit val PhoneSchema: Schema.Typeclass[Phone] =
    Schema.stringSchema.contramap[Phone](identity(_))
  implicit val URLSchema: Schema.Typeclass[URL] =
    Schema.stringSchema.contramap[URL](_.toString)
  implicit val PrivilegeSchema: Schema.Typeclass[Privilege] = Schema.gen
  implicit val AssetSchema: Schema.Typeclass[Asset] = Schema.gen
  implicit val AuthTokensSchema: Schema.Typeclass[AuthTokens] = Schema.gen
  implicit val RoleSchema: Schema.Typeclass[Role] = Schema.gen
  implicit lazy val AuthedUserSchema: Schema.Typeclass[AuthedUser] =
    obj("AuthedUser", Some("A user of the service"))(implicit ft =>
      List(
        field("id")(_.id),
        field("role")(_.role),
        field("login")(_.login),
        field("phone")(_.phone),
        field("fullName")(_.fullName),
      )
    )
}
