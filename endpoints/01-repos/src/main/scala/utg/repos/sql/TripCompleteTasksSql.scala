package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripCompleteTaskId
import utg.domain.TripId

private[repos] object TripCompleteTasksSql extends Sql[TripCompleteTaskId] {
  private[repos] val codec: Codec[dto.TripCompleteTask] =
    (id *: zonedDateTime *: TripsSql.id *: nonNegInt *: nes *: zonedDateTime *: nes *: AssetsSql.id
      *: UsersSql.id *: bool).to[dto.TripCompleteTask]

  val insert: Command[dto.TripCompleteTask] =
    sql"""INSERT INTO trip_complete_tasks VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripCompleteTask] =
    sql"""SELECT * FROM trip_complete_tasks
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)
}
