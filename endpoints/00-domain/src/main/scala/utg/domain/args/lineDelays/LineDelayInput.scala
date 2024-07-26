package utg.domain.args.lineDelays

import java.time.ZonedDateTime
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import utg.domain.{SignId, TripId}

@JsonCodec
case class LineDelayInput(
    tripId: TripId,
    name: NonEmptyString,
    startTime: ZonedDateTime,
    endTime: ZonedDateTime,
    signId: SignId,
  )
