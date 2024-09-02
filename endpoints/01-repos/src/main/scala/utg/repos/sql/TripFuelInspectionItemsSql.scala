package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripFuelInspectionId
import utg.domain.TripFuelInspectionItemId

private[repos] object TripFuelInspectionItemsSql extends Sql[TripFuelInspectionItemId] {
  private[repos] val codec: Codec[dto.TripFuelInspectionItem] =
    (id *: TripFuelInspectionsSql.id *: fuelType *: nonNegDouble *: bool)
      .to[dto.TripFuelInspectionItem]

  val insert: Command[dto.TripFuelInspectionItem] =
    sql"""INSERT INTO trip_fuel_inspection_items VALUES ($codec)""".command

  val selectById: Query[TripFuelInspectionId, dto.TripFuelInspectionItem] =
    sql"""SELECT * FROM trip_fuel_inspection_items 
        WHERE deleted = false AND trip_fuel_inspection_id = ${TripFuelInspectionsSql.id}"""
      .query(codec)
}
