package utg.domain.args.smsMessages

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone
import utg.domain.enums.DeliveryStatus

@JsonCodec
case class SmsMessageInput(
    phone: Phone,
    text: NonEmptyString,
    status: DeliveryStatus,
  )
