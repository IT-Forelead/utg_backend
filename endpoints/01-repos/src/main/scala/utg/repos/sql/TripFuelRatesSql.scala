package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripFuelRateId
import utg.domain.TripId

private[repos] object TripFuelRatesSql extends Sql[TripFuelRateId] {
  private[repos] val codec: Codec[dto.TripFuelRate] =
    (id *: zonedDateTime *: TripsSql.id *: fuelType *: nonNegDouble *: nonNegDouble *: nonNegDouble
      *: UsersSql.id *: AssetsSql.id *: bool).to[dto.TripFuelRate]

  val insert: Command[dto.TripFuelRate] =
    sql"""INSERT INTO trip_fuel_rates VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripFuelRate] =
    sql"""SELECT * FROM trip_fuel_rates
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)
}
