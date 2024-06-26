package uz.scala.integration.sms.domain

import StatusResponse.SmsStatus
import io.circe.generic.JsonCodec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec

@JsonCodec
case class StatusResponse(
    messages: List[SmsStatus]
  )

object StatusResponse {
  @ConfiguredJsonCodec
  case class SmsStatus(
      messageId: Int,
      channel: String,
      status: DeliveryStatus,
    )

  object SmsStatus {
    implicit val configuration: Configuration = Configuration.default.withKebabCaseMemberNames
  }
}
