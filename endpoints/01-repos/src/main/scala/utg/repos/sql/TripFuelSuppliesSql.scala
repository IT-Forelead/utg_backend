package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripFuelSupplyId
import utg.domain.TripId

private[repos] object TripFuelSuppliesSql extends Sql[TripFuelSupplyId] {
  private[repos] val codec: Codec[dto.TripFuelSupply] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: UsersSql.id *: AssetsSql.id *: bool)
      .to[dto.TripFuelSupply]

  val insert: Command[dto.TripFuelSupply] =
    sql"""INSERT INTO trip_fuel_supplies VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripFuelSupply] =
    sql"""SELECT * FROM trip_fuel_supplies
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)
}
