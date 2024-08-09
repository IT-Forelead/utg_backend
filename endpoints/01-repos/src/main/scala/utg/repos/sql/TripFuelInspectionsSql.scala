package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripFuelInspectionId

private[repos] object TripFuelInspectionsSql extends Sql[TripFuelInspectionId] {
  private[repos] val codec: Codec[dto.TripFuelInspection] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: vehicleIndicatorActionType *: nonNegDouble
      *: UsersSql.id *: AssetsSql.id *: bool).to[dto.TripFuelInspection]

  val insert: Command[dto.TripFuelInspection] =
    sql"""INSERT INTO trip_fuel_inspections VALUES ($codec)""".command
}
