package utg.domain.args.smsMessages

import java.time.ZonedDateTime

import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.Phone
import utg.domain.enums.DeliveryStatus

@JsonCodec
case class SmsMessageFilters(
    phone: Option[Phone] = None,
    status: Option[DeliveryStatus] = None,
    from: Option[ZonedDateTime] = None,
    to: Option[ZonedDateTime] = None,
    limit: Option[NonNegInt] = None,
    offset: Option[NonNegInt] = None,
  )
