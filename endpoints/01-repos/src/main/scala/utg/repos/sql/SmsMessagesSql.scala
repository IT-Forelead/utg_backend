package utg.repos.sql

import java.time.ZonedDateTime

import skunk._
import skunk.codec.all.bool
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.SmsMessageId
import utg.domain.args.smsMessages.SmsMessageFilters
import utg.domain.enums.DeliveryStatus

private[repos] object SmsMessagesSql extends Sql[SmsMessageId] {
  private[repos] val codec =
    (id *: zonedDateTime *: phone *: nes *: deliveryStatus *: zonedDateTime.opt *: bool)
      .to[dto.SmsMessage]

  val insert: Command[dto.SmsMessage] =
    sql"""INSERT INTO sms_messages VALUES ($codec)""".command

  def get(filters: SmsMessageFilters): AppliedFragment = {
    val searchFilters = List(
      filters.phone.map(sql"phone = $phone"),
      filters.status.map(sql"status = $deliveryStatus"),
      filters.from.map(sql"created_at >= $zonedDateTime"),
      filters.to.map(sql"created_at <= $zonedDateTime"),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT *, COUNT(*) OVER() AS total FROM sms_messages
           WHERE deleted = false AND created_at DESC""".apply(Void)
    baseQuery.andOpt(searchFilters)
  }

  val changeStatus: Command[DeliveryStatus *: Option[ZonedDateTime] *: SmsMessageId *: EmptyTuple] =
    sql"""UPDATE sms_messages SET status = $deliveryStatus, updated_at = ${zonedDateTime.opt} WHERE id = $id""".command
}
