package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripGivenFuelId

private[repos] object TripGivenFuelsSql extends Sql[TripGivenFuelId] {
  private[repos] val codec: Codec[dto.TripGivenFuel] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: nes *: nes *: nonNegDouble
      *: UsersSql.id *: AssetsSql.id *: bool).to[dto.TripGivenFuel]

  val insert: Command[dto.TripGivenFuel] =
    sql"""INSERT INTO trip_given_fuels VALUES ($codec)""".command
}
