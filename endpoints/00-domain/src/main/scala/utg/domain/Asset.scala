package utg.domain

import java.net.URL
import java.time.ZonedDateTime
import eu.timepit.refined.types.string.NonEmptyString

case class Asset(
    id: AssetId,
    createdAt: ZonedDateTime,
    s3Key: NonEmptyString,
    fileName: Option[NonEmptyString],
    contentType: Option[NonEmptyString],
  )

object Asset {
  case class AssetInfo(
      filename: Option[NonEmptyString],
      contentType: Option[NonEmptyString],
      extension: NonEmptyString,
      url: URL,
    )
}
