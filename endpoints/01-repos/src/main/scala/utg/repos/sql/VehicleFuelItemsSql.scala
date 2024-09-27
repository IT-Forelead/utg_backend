package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.VehicleFuelItemId
import utg.domain.VehicleId

private[repos] object VehicleFuelItemsSql extends Sql[VehicleFuelItemId] {
  private[repos] val codec: Codec[dto.VehicleFuelItem] =
    (id *: VehiclesSql.id *: fuelType *: nonNegDouble *: bool)
      .to[dto.VehicleFuelItem]

  val insert: Command[dto.VehicleFuelItem] =
    sql"""INSERT INTO vehicle_fuel_items VALUES ($codec)""".command

  val selectByVehicleId: Query[VehicleId, dto.VehicleFuelItem] =
    sql"""SELECT * FROM vehicle_fuel_items WHERE deleted = false AND vehicle_id = ${VehiclesSql.id}"""
      .query(codec)

  def findByVehicleIds(ids: List[VehicleId]): Query[ids.type, dto.VehicleFuelItem] =
    sql"""SELECT * FROM vehicle_fuel_items WHERE vehicle_id IN (${VehiclesSql.id.values.list(ids)})"""
      .query(codec)
}
