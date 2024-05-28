package uz.scala.integration.sms.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec

import utg.Phone

@JsonCodec
case class SMS(
    phone: String,
    text: String,
  )

object SMS {
  def unPlus(phone: Phone, text: NonEmptyString): SMS =
    SMS(phone.value.replace("+", ""), text.value)
}
