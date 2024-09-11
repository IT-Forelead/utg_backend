package utg.algebras

import cats.MonadThrow
import cats.implicits._
import uz.scala.integration.sms
import uz.scala.integration.sms.OperSmsClient

import utg.domain.ResponseData
import utg.domain.SmsMessage
import utg.domain.SmsMessageId
import utg.domain.args.smsMessages._
import utg.domain.enums.DeliveryStatus
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.SmsMessagesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait SmsMessagesAlgebra[F[_]] {
  def create(input: SmsMessageInput): F[SmsMessage]
  def get(filters: SmsMessageFilters): F[ResponseData[SmsMessage]]
}

object SmsMessagesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      smsMessagesRepository: SmsMessagesRepository[F],
      opersms: OperSmsClient[F],
    ): SmsMessagesAlgebra[F] =
    new SmsMessagesAlgebra[F] {
      def deliveryStatusTransformer(status: sms.domain.DeliveryStatus): DeliveryStatus =
        status match {
          case sms.domain.DeliveryStatus.Sent => DeliveryStatus.Sent
          case sms.domain.DeliveryStatus.Delivered => DeliveryStatus.Delivered
          case sms.domain.DeliveryStatus.NotDelivered => DeliveryStatus.NotDelivered
          case sms.domain.DeliveryStatus.Failed => DeliveryStatus.Failed
          case sms.domain.DeliveryStatus.Transmitted => DeliveryStatus.Transmitted
          case sms.domain.DeliveryStatus.Undefined => DeliveryStatus.Undefined
        }

      private def changeStatus(id: SmsMessageId): sms.domain.DeliveryStatus => F[Unit] = status =>
        for {
          now <- Calendar[F].currentZonedDateTime
          deliveryStatus = deliveryStatusTransformer(status)
          updatedInput = UpdateSmsMessageInput(id, now, deliveryStatus)
          _ <- smsMessagesRepository.changeStatus(updatedInput).void
        } yield ()

      override def create(input: SmsMessageInput): F[SmsMessage] =
        for {
          id <- ID.make[F, SmsMessageId]
          now <- Calendar[F].currentZonedDateTime
          dtoSmsMessage = dto.SmsMessage(
            id = id,
            createdAt = now,
            phone = input.phone,
            text = input.text,
            status = input.status,
          )
          _ <- smsMessagesRepository.create(dtoSmsMessage)
          _ <- opersms.send(input.phone, input.text, changeStatus(dtoSmsMessage.id))
        } yield dtoSmsMessage.toDomain

      override def get(filters: SmsMessageFilters): F[ResponseData[SmsMessage]] =
        for {
          dtoSmsMessages <- smsMessagesRepository.get(filters)
          smsMessages = dtoSmsMessages.data.map(_.toDomain)
        } yield dtoSmsMessages.copy(data = smsMessages)
    }
}
