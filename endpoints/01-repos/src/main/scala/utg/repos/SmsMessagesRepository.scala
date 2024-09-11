package utg.repos

import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._

import utg.domain.ResponseData
import utg.domain.args.smsMessages._
import utg.repos.sql.SmsMessagesSql
import utg.repos.sql.dto

trait SmsMessagesRepository[F[_]] {
  def create(input: dto.SmsMessage): F[Unit]
  def get(filters: SmsMessageFilters): F[ResponseData[dto.SmsMessage]]
  def changeStatus(input: UpdateSmsMessageInput): F[Unit]
}

object SmsMessagesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): SmsMessagesRepository[F] = new SmsMessagesRepository[F] {
    override def create(input: dto.SmsMessage): F[Unit] =
      SmsMessagesSql.insert.execute(input)

    override def get(filters: SmsMessageFilters): F[ResponseData[dto.SmsMessage]] = {
      val af = SmsMessagesSql.get(filters).paginateOpt(filters.limit, filters.offset)
      af.fragment
        .query(SmsMessagesSql.codec *: int8)
        .queryList(af.argument)
        .map { data =>
          val list = data.map(_.head)
          val count = data.headOption.fold(0L)(_.tail.head)
          ResponseData(list, count)
        }
    }

    override def changeStatus(input: UpdateSmsMessageInput): F[Unit] =
      SmsMessagesSql
        .changeStatus
        .execute(input.status *: input.updatedAt.some *: input.id *: EmptyTuple)
  }
}
