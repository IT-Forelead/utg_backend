package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone
import utg.domain.enums.DeliveryStatus

@JsonCodec
case class SmsMessage(
    id: SmsMessageId,
    createdAt: ZonedDateTime,
    phone: Phone,
    text: NonEmptyString,
    status: DeliveryStatus,
    updatedAt: Option[ZonedDateTime],
  )
