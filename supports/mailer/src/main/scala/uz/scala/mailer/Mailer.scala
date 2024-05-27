package uz.scala.mailer

import java.util.Properties
import javax.mail.Message.RecipientType._
import javax.mail._
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.DurationLong
import scala.jdk.CollectionConverters.MapHasAsJava

import cats.effect.Async
import cats.effect.Sync
import cats.effect.implicits.genSpawnOps
import cats.implicits._
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.cats.refTypeShow
import org.typelevel.log4cats.Logger
import retry.RetryPolicies.exponentialBackoff
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import uz.scala.mailer.data.Email
import uz.scala.mailer.data.Html
import uz.scala.mailer.data.Props
import uz.scala.mailer.data.Props.SmtpConnectionTimeoutKey
import uz.scala.mailer.data.Text
import uz.scala.mailer.exception.DeliverFailure.AuthenticationFailed
import uz.scala.mailer.exception.InvalidAddress
import uz.scala.mailer.retries.Retry

import utg.EmailAddress

trait Mailer[F[_]] {
  def send(email: Email): F[Unit]
}
object Mailer {
  def make[F[_]: Async: Logger](config: MailerConfig): Mailer[F] =
    if (config.enabled)
      new MailerImpl[F](
        Props.default.withSmtpAddress(config.host, config.port),
        config,
      )
    else new NoOpMailerImpl[F](config.fromAddress)

  private class NoOpMailerImpl[F[_]: Logger](from: EmailAddress) extends Mailer[F] {
    override def send(email: Email): F[Unit] =
      Logger[F].info(
        s"""Email sent from [ ${Console.GREEN} $from ${Console.RESET} ] to ${Console.GREEN}
          ${email.to.mkString_("[ ", ", ", " ]")} ${Console.RESET}
          ${email.content.text.fold("") { text =>
            s"email text [ \n${text.value}\n ]"
          }}
          ${email.content.html.fold("") { html =>
            s"email html [ \n${html.value}\n ]"
          }}"""
      )
  }

  private class MailerImpl[F[_]: Async](
      props: Props,
      config: MailerConfig,
    )(implicit
      logger: Logger[F],
      F: Sync[F],
    ) extends Mailer[F] {
    private[mailer] val retryPolicy: RetryPolicy[F] = {
      val delay = props.values.get(SmtpConnectionTimeoutKey).fold(1.second)(_.toLong.millis)
      limitRetries[F](5) |+| exponentialBackoff[F](delay)
    }

    private[mailer] val properties: Properties = {
      val properties = System.getProperties
      properties.putAll(props.values.asJava)
      properties
    }

    private[mailer] def makeSession(properties: Properties): Session =
      Session.getInstance(
        properties,
        new Authenticator {
          override def getPasswordAuthentication: PasswordAuthentication =
            new PasswordAuthentication(config.fromAddress.value, config.password.value)
        },
      )

    private[mailer] def prepTextPart(text: Text): MimeBodyPart = {
      val part = new MimeBodyPart()
      part.setText(text.value, text.charset.toString, text.subtype.value)
      text.headers.foreach(header => part.setHeader(header.name, header.value))
      part
    }

    private[mailer] def prepHtmlPart(html: Html): MimeBodyPart = {
      val part = new MimeBodyPart()
      part.setText(html.value, html.charset.toString, html.subtype.value)
      html.headers.foreach(header => part.setHeader(header.name, header.value))
      part
    }

    private[mailer] def prepareMessage(session: Session, email: Email): MimeMessage = {
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(config.fromAddress.value))
      email.to.map(ads => message.addRecipient(TO, new InternetAddress(ads.value)))
      email.cc.foreach(ads => message.addRecipient(CC, new InternetAddress(ads.value)))
      email.bcc.foreach(ads => message.addRecipient(BCC, new InternetAddress(ads.value)))
      message.setSubject(email.subject.value)
      val bodyParts = List(
        email.content.text.map(prepTextPart),
        email.content.html.map(prepHtmlPart),
      ).flatten
      message.setContent(new MimeMultipart {
        bodyParts.foreach(addBodyPart)
      })
      email.headers.foreach(header => message.setHeader(header.name, header.value))
      message
    }

    override def send(email: Email): F[Unit] =
      (for {
        _ <- Logger[F].info(
          Console.GREEN + s"Starting sending email: from [${config.fromAddress}] subject [${email.subject}]" + Console.RESET
        )
        session = makeSession(properties)
        message = prepareMessage(session, email)
        task = F.delay(Transport.send(message))
        result <- Retry[F]
          .retry(retryPolicy)(task)
          .adaptError {
            case exception: AuthenticationFailedException =>
              AuthenticationFailed(exception.getMessage)
            case exception: SendFailedException =>
              InvalidAddress(exception.getMessage)
          }
        _ <- Logger[F].info(
          Console.GREEN + s"Finished sending email: from [${config.fromAddress}] subject [${email.subject}]" + Console.RESET
        )
      } yield result).start.map(_.join).void
  }
}
