package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripVehicleIndicatorId

private[repos] object TripVehicleIndicatorsSql extends Sql[TripVehicleIndicatorId] {
  private[repos] val codec: Codec[dto.TripVehicleIndicator] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: vehicleIndicatorActionType *: zonedDateTime *: zonedDateTime
      *: nonNegDouble *: nonNegDouble *: bool).to[dto.TripVehicleIndicator]

  val insert: Command[dto.TripVehicleIndicator] =
    sql"""INSERT INTO trip_vehicle_indicators VALUES ($codec)""".command
}
