package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class LineDelay(
    id: LineDelayId,
    name: NonEmptyString,
    startTime: ZonedDateTime,
    endTime: ZonedDateTime,
    signId: SignId,
  )
