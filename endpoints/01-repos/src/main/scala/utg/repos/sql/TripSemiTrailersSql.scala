package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripId
import utg.domain.TripSemiTrailerId

private[repos] object TripSemiTrailersSql extends Sql[TripSemiTrailerId] {
  private[repos] val codec =
    (id *: TripsSql.id *: VehiclesSql.id *: bool).to[dto.TripSemiTrailer]

  def insert(item: List[dto.TripSemiTrailer]): Command[item.type] =
    sql"""INSERT INTO trip_semi_trailers VALUES ${codec.values.list(item)}""".command

  def findByTripIds(ids: List[TripId]): Query[ids.type, dto.TripSemiTrailer] =
    sql"""SELECT * FROM trip_semi_trailers WHERE trip_id IN (${TripsSql.id.values.list(ids)})"""
      .query(codec)

  val deleteByTripId: Command[TripId] =
    sql"""DELETE FROM trip_semi_trailers WHERE trip_id = ${TripsSql.id}""".command
}
