package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripVehicleIndicatorId

private[repos] object TripVehicleIndicatorsSql extends Sql[TripVehicleIndicatorId] {
  private[repos] val codec: Codec[dto.TripVehicleIndicator] =
    (id *: id /*TODO second id should be TripSql.id*/ *: VehiclesSql.id *: zonedDateTime *: vehicleIndicatorType *: zonedDateTime *: nes.opt
      *: nes *: zonedDateTime *: bool).to[dto.TripVehicleIndicator]

  val insert: Command[dto.TripVehicleIndicator] =
    sql"""INSERT INTO trip_vehicle_indicators VALUES ($codec)""".command

}
