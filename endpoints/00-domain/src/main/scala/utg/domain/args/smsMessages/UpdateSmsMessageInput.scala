package utg.domain.args.smsMessages

import java.time.ZonedDateTime

import io.circe.generic.JsonCodec

import utg.domain.SmsMessageId
import utg.domain.enums.DeliveryStatus

@JsonCodec
case class UpdateSmsMessageInput(
    id: SmsMessageId,
    updatedAt: ZonedDateTime,
    status: DeliveryStatus,
  )
