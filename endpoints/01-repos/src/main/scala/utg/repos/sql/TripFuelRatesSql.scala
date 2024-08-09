package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripFuelRateId

private[repos] object TripFuelRatesSql extends Sql[TripFuelRateId] {
  private[repos] val codec: Codec[dto.TripFuelRate] =
    (id *: zonedDateTime *: TripsSql.id *: nonNegDouble *: nonNegDouble *: nonNegDouble
      *: UsersSql.id *: AssetsSql.id *: bool).to[dto.TripFuelRate]

  val insert: Command[dto.TripFuelRate] =
    sql"""INSERT INTO trip_fuel_rates VALUES ($codec)""".command
}
