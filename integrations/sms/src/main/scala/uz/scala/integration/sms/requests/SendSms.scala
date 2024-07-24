package uz.scala.integration.sms.requests

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString
import sttp.model.Method
import uz.scala.integration.sms.domain.SMS
import uz.scala.integration.sms.domain.SmsResponse
import uz.scala.sttp.SttpRequest
import uz.scala.syntax.all.genericSyntaxGenericTypeOps
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV
case class SendSms(
    login: NonEmptyString,
    password: NonEmptyString,
    appDomain: NonEmptyString,
    sms: NonEmptyList[SMS],
  )

object SendSms {
  implicit val sttpRequest: SttpRequest[SendSms, List[SmsResponse]] =
    new SttpRequest[SendSms, List[SmsResponse]] {
      val method: Method = Method.POST
      val path: Path = _ => ""
      def body: Body = formBody { req =>
        Map(
          "login" -> req.login,
          "password" -> req.password.value,
          "data" -> req
            .sms
            .map(s =>
              s.copy(
                text = s.text.replace("%%UTG_DOMAIN%%", req.appDomain.value),
              )
            )
            .toJson,
        )
      }
    }
}
