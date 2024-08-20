package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripId
import utg.domain.TripRouteDelayId

private[repos] object TripRouteDelaysSql extends Sql[TripRouteDelayId] {
  private[repos] val codec: Codec[dto.TripRouteDelay] =
    (id *: zonedDateTime *: TripsSql.id *: nes *: zonedDateTime *: zonedDateTime *: UsersSql.id *: AssetsSql.id *: bool)
      .to[dto.TripRouteDelay]

  val insert: Command[dto.TripRouteDelay] =
    sql"""INSERT INTO trip_route_delays VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripRouteDelay] =
    sql"""SELECT * FROM trip_route_delays
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)
}
