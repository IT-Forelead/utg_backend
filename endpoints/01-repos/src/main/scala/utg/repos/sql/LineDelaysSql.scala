package utg.repos.sql

import skunk._
import skunk.codec.all.{bool, varchar}
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import utg.domain.LineDelayId
import utg.domain.args.lineDelays.LineDelayFilters

private[repos] object LineDelaysSql extends Sql[LineDelayId] {
  private[repos] val codec =
    (id *: zonedDateTime *: TripsSql.id *: nes *: zonedDateTime *: zonedDateTime *: signId *: bool)
      .to[dto.LineDelay]

  val findById: Query[LineDelayId, dto.LineDelay] =
    sql"""SELECT id, name, start_time, end_time, sign_id FROM line_delays
          WHERE id = $id LIMIT 1""".query(codec)

  val insert: Command[dto.LineDelay] =
    sql"""INSERT INTO line_delays VALUES ($id,  $nes, $zonedDateTime, $zonedDateTime, $signId)"""
      .command
      .contramap { (l: dto.LineDelay) =>
        l.id *: l.name *: l.startTime *: l.endTime *: l.signId *: EmptyTuple
      }

  val update: Command[dto.LineDelay] =
    sql"""UPDATE line_delays
       SET name = $nes,
       start_time = $zonedDateTime,
       end_time = $zonedDateTime,
       sign_id = $signId,
       WHERE id = $id
     """
      .command
      .contramap {
        case lineDelay: dto.LineDelay =>
          lineDelay.name *: lineDelay.startTime *: lineDelay.endTime *: lineDelay.signId *: lineDelay.id *: EmptyTuple
      }

  private def searchFilter(filters: LineDelayFilters): List[Option[AppliedFragment]] =
    List(
      filters.name.map(s => s"%$s%").map(sql"l.name ILIKE $varchar")
    )

  def select(filters: LineDelayFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT name, start_time, end_time, sign_id, COUNT(*) OVER() AS total FROM line_delays WHERE deleted = false"""
    baseQuery(Void).andOpt(searchFilter(filters))
  }

  def delete: Command[LineDelayId] =
    sql"""DELETE FROM line_delays l WHERE l.id = $id""".command
}
