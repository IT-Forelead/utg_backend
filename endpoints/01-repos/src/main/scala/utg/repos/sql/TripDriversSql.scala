package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripDriverId
import utg.domain.TripId

private[repos] object TripDriversSql extends Sql[TripDriverId] {
  private[repos] val codec =
    (id *: TripsSql.id *: UsersSql.id *: nes *: bool).to[dto.TripDriver]

  def insert(item: List[dto.TripDriver]): Command[item.type] =
    sql"""INSERT INTO trip_drivers VALUES ${codec.values.list(item)}""".command

  val selectByTripId: Query[TripId, dto.TripDriver] =
    sql"""SELECT * FROM trip_drivers WHERE deleted = false AND trip_id = ${TripsSql.id}"""
      .query(codec)

  def findByTripIds(ids: List[TripId]): Query[ids.type, dto.TripDriver] =
    sql"""SELECT * FROM trip_drivers WHERE trip_id IN (${TripsSql.id.values.list(ids)})"""
      .query(codec)

  val deleteByTripIdSql: Command[TripId] =
    sql"""DELETE FROM trip_drivers WHERE trip_id = ${TripsSql.id}""".command
}
