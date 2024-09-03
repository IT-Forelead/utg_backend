package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripFuelSupplyId
import utg.domain.TripFuelSupplyItemId

private[repos] object TripFuelSupplyItemsSql extends Sql[TripFuelSupplyItemId] {
  private[repos] val codec: Codec[dto.TripFuelSupplyItem] =
    (id *: TripFuelSuppliesSql.id *: fuelType *: nonNegDouble *: bool)
      .to[dto.TripFuelSupplyItem]

  val insert: Command[dto.TripFuelSupplyItem] =
    sql"""INSERT INTO trip_fuel_supply_items VALUES ($codec)""".command

  val selectByTripFuelSupplyId: Query[TripFuelSupplyId, dto.TripFuelSupplyItem] =
    sql"""SELECT * FROM trip_fuel_supply_items
         WHERE deleted = false AND trip_fuel_supply_id = ${TripFuelSuppliesSql.id}""".query(codec)
}
