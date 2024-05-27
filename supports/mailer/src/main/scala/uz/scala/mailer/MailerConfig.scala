package uz.scala.mailer

import eu.timepit.refined.types.net.SystemPortNumber
import uz.scala.mailer.data.types.Host
import uz.scala.mailer.data.types.Password

import utg.EmailAddress

case class MailerConfig(
    enabled: Boolean,
    host: Host,
    port: SystemPortNumber,
    username: EmailAddress,
    password: Password,
    fromAddress: EmailAddress,
    recipients: List[EmailAddress],
  )
