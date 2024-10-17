package utg.domain

import java.net.URL
import java.time.ZonedDateTime

import scala.util.Try

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class Asset(
    id: AssetId,
    createdAt: ZonedDateTime,
    s3Key: NonEmptyString,
    fileName: Option[NonEmptyString],
    contentType: Option[NonEmptyString],
  )

object Asset {
  implicit val encodeUrl: Encoder[URL] = Encoder.encodeString.contramap[URL](_.toString)
  implicit val decodeUrl: Decoder[URL] = Decoder.decodeString.emap { str =>
    Try(new URL(str)).toEither.left.map(_ => "URL decoding error")
  }
  @JsonCodec
  case class AssetInfo(
      id: AssetId,
      filename: Option[NonEmptyString],
      contentType: Option[NonEmptyString],
      extension: NonEmptyString,
      url: URL,
    )
}
