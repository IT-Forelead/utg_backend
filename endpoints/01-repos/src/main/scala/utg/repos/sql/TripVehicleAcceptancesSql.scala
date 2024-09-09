package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripId
import utg.domain.TripVehicleAcceptanceId

private[repos] object TripVehicleAcceptancesSql extends Sql[TripVehicleAcceptanceId] {
  private[repos] val codec: Codec[dto.TripVehicleAcceptance] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: vehicleIndicatorActionType *: conditionType
      *: UsersSql.id.opt *: AssetsSql.id.opt *: UsersSql.id.opt *: AssetsSql.id.opt *: bool)
      .to[dto.TripVehicleAcceptance]

  val insert: Command[dto.TripVehicleAcceptance] =
    sql"""INSERT INTO trip_vehicle_acceptances VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripVehicleAcceptance] =
    sql"""SELECT * FROM trip_vehicle_acceptances
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at ASC, action_type ASC""".query(codec)
}
