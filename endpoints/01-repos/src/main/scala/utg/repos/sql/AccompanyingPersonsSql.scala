package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._
import utg.domain.{AccompanyingPersonId, TripId}

private[repos] object AccompanyingPersonsSql extends Sql[AccompanyingPersonId] {
  private[repos] val codec =
    (id *: TripsSql.id *: UsersSql.id *: bool).to[dto.AccompanyingPerson]

  def insert(item: List[dto.AccompanyingPerson]): Command[item.type] =
    sql"""INSERT INTO trip_accompanying_persons VALUES ${codec.values.list(item)}""".command

  def findByTripIds(ids: List[TripId]): Query[ids.type, dto.AccompanyingPerson] =
    sql"""SELECT * FROM trip_accompanying_persons WHERE trip_id IN (${TripsSql.id.values.list(ids)})""".query(codec)
}
