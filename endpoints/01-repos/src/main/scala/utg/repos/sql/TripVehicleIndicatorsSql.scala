package utg.repos.sql

import eu.timepit.refined.types.all.NonNegDouble
import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripId
import utg.domain.TripVehicleIndicatorId
import utg.domain.VehicleId

private[repos] object TripVehicleIndicatorsSql extends Sql[TripVehicleIndicatorId] {
  private[repos] val codec: Codec[dto.TripVehicleIndicator] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: vehicleIndicatorActionType *: zonedDateTime *: zonedDateTime
      *: nonNegDouble *: nonNegDouble.opt *: nonNegDouble *: bool).to[dto.TripVehicleIndicator]

  val insert: Command[dto.TripVehicleIndicator] =
    sql"""INSERT INTO trip_vehicle_indicators VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripVehicleIndicator] =
    sql"""SELECT * FROM trip_vehicle_indicators
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at ASC, action_type ASC""".query(codec)

  val selectLastOdometerIndicatorByVehicleId: Query[VehicleId, NonNegDouble] =
    sql"""SELECT odometer_indicator FROM trip_vehicle_indicators
         WHERE deleted = false AND action_type = 'back'
         AND deleted = false AND vehicle_id = ${VehiclesSql.id}
         ORDER BY created_at DESC LIMIT 1""".query(nonNegDouble)
}
