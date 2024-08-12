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
}
