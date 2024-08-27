package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripId
import utg.domain.TripTrailerId

private[repos] object TripTrailersSql extends Sql[TripTrailerId] {
  private[repos] val codec =
    (id *: TripsSql.id *: VehiclesSql.id *: bool).to[dto.TripTrailer]

  def insert(item: List[dto.TripTrailer]): Command[item.type] =
    sql"""INSERT INTO trip_trailers VALUES ${codec.values.list(item)}""".command

  def findByTripIds(ids: List[TripId]): Query[ids.type, dto.TripTrailer] =
    sql"""SELECT * FROM trip_trailers WHERE trip_id IN (${TripsSql.id.values.list(ids)})"""
      .query(codec)

  val deleteByTripId: Command[TripId] =
    sql"""DELETE FROM trip_trailers WHERE trip_id = ${TripsSql.id}""".command
}
