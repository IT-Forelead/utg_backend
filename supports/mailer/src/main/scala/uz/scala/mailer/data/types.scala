package uz.scala.mailer.data

import cats.implicits.catsSyntaxEitherId
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import pureconfig.ConfigReader
import pureconfig.error.FailureReason
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

object types {
  @newtype case class Subtype(value: String)
  @newtype case class Protocol(value: String)
  @newtype case class Subject(value: NonEmptyString)

  @newtype case class Host(value: NonEmptyString)
  object Host {
    implicit val reader: ConfigReader[Host] =
      ConfigReader.fromString[Host](str => Host(str).asRight[FailureReason])
  }

  @newtype case class Password(value: NonEmptyString)
  object Password {
    implicit val reader: ConfigReader[Password] =
      ConfigReader.fromString[Password](str => Password(str).asRight[FailureReason])
  }

  object Subtype {
    val HTML: Subtype = Subtype("html")
    val PLAIN: Subtype = Subtype("plain")
  }
  object Protocol {
    val Smtp: Protocol = Protocol("smtp")
    val Imap: Protocol = Protocol("imap")
  }
}
