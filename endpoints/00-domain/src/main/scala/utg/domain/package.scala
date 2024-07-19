package utg

import java.util.UUID

import scala.concurrent.duration.FiniteDuration

import cats.implicits.catsSyntaxEitherId
import derevo.cats.eqv
import derevo.cats.show
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.Encoder
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.toCoercibleIdOps
import pureconfig.BasicReaders.finiteDurationConfigReader
import pureconfig.ConfigReader
import pureconfig.error.FailureReason
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import uz.scala.syntax.refined.commonSyntaxAutoRefineV
import java.security.MessageDigest
import java.math.BigInteger

import utg.utils.uuid

package object domain {
  @derive(eqv, show, uuid)
  @newtype case class RoleId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class UserId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class AssetId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class VehicleId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class RegionId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class BranchId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class VehicleCategoryId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class TripId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class AccompanyingPersonId(value: UUID)
  @newtype case class JwtAccessTokenKey(secret: NonEmptyString)
  @derive(eqv, show, uuid)
  @newtype case class TripVehicleIndicatorId(value: UUID)
  @derive(eqv, show, uuid)
  @newtype case class TripId(value: UUID)

  object JwtAccessTokenKey {
    implicit val reader: ConfigReader[JwtAccessTokenKey] =
      ConfigReader.fromString[JwtAccessTokenKey](str =>
        JwtAccessTokenKey(str).asRight[FailureReason]
      )
  }

  @newtype case class TokenExpiration(value: FiniteDuration)

  object TokenExpiration {
    implicit val reader: ConfigReader[TokenExpiration] =
      finiteDurationConfigReader.map(duration => TokenExpiration(duration))
  }

  implicit val passwordHashEncoder: Encoder[PasswordHash[SCrypt]] =
    Encoder.encodeString.contramap(_.toString)
  implicit val passwordHashDecoder: Decoder[PasswordHash[SCrypt]] =
    Decoder.decodeString.map(PasswordHash[SCrypt])

  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.asInstanceOf[B])

  def generateShortHash(input: String): String = {
    val md = MessageDigest.getInstance("SHA-256")
    val hashBytes = md.digest(input.getBytes("UTF-8"))
    val hashString = new BigInteger(1, hashBytes).toString(36)  // Using base-36 encoding
    hashString.take(6)
  }

}
