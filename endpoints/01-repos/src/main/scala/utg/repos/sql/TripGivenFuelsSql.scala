package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripGivenFuelId
import utg.domain.TripId

private[repos] object TripGivenFuelsSql extends Sql[TripGivenFuelId] {
  private[repos] val codec: Codec[dto.TripGivenFuel] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: nes *: nes *: nonNegDouble
      *: UsersSql.id *: AssetsSql.id *: bool).to[dto.TripGivenFuel]

  val insert: Command[dto.TripGivenFuel] =
    sql"""INSERT INTO trip_given_fuels VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripGivenFuel] =
    sql"""SELECT * FROM trip_given_fuels
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)
}
