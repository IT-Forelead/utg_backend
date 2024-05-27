package uz.scala.mailer.data

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import eu.timepit.refined.types.string.NonEmptyString
import uz.scala.mailer.data.types.Subtype
import uz.scala.mailer.data.types.Subtype.HTML

case class Html(
    value: NonEmptyString,
    charset: Charset = StandardCharsets.UTF_8,
    subtype: Subtype = HTML,
    headers: List[Header] = Nil,
  )
