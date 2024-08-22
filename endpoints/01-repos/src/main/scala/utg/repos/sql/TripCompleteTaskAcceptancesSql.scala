package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripCompleteTaskAcceptanceId
import utg.domain.TripId

private[repos] object TripCompleteTaskAcceptancesSql extends Sql[TripCompleteTaskAcceptanceId] {
  private[repos] val codec: Codec[dto.TripCompleteTaskAcceptance] =
    (id *: zonedDateTime *: TripsSql.id *: nonNegInt *: nonNegInt *: nes *: AssetsSql.id.opt
      *: UsersSql.id.opt *: AssetsSql.id.opt *: UsersSql.id.opt *: AssetsSql.id.opt *: bool)
      .to[dto.TripCompleteTaskAcceptance]

  val insert: Command[dto.TripCompleteTaskAcceptance] =
    sql"""INSERT INTO trip_complete_task_acceptances VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripCompleteTaskAcceptance] =
    sql"""SELECT * FROM trip_complete_task_acceptances
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)
}
