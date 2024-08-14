package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripAccompanyingPersonId
import utg.domain.TripId

private[repos] object TripAccompanyingPersonsSql extends Sql[TripAccompanyingPersonId] {
  private[repos] val codec =
    (id *: TripsSql.id *: UsersSql.id *: bool).to[dto.TripAccompanyingPerson]

  def insert(item: List[dto.TripAccompanyingPerson]): Command[item.type] =
    sql"""INSERT INTO trip_accompanying_persons VALUES ${codec.values.list(item)}""".command

  def findByTripIds(ids: List[TripId]): Query[ids.type, dto.TripAccompanyingPerson] =
    sql"""SELECT * FROM trip_accompanying_persons WHERE trip_id IN (${TripsSql.id.values.list(ids)})"""
      .query(codec)

  val deleteByTripId: Command[TripId] =
    sql"""DELETE FROM trip_accompanying_persons WHERE trip_id = ${TripsSql.id}""".command
}
