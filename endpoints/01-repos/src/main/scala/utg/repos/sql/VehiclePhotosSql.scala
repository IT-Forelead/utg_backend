package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.VehicleId
import utg.domain.VehiclePhotoId

private[repos] object VehiclePhotosSql extends Sql[VehiclePhotoId] {
  private[repos] val codec: Codec[dto.VehiclePhoto] =
    (id *: VehiclesSql.id *: AssetsSql.id *: bool).to[dto.VehiclePhoto]

  val insert: Command[dto.VehiclePhoto] =
    sql"""INSERT INTO vehicle_photos VALUES ($codec)""".command

  val selectByVehicleId: Query[VehicleId, dto.VehiclePhoto] =
    sql"""SELECT * FROM vehicle_photos WHERE deleted = false AND vehicle_id = ${VehiclesSql.id}"""
      .query(codec)

  def findByVehicleIds(ids: List[VehicleId]): Query[ids.type, dto.VehiclePhoto] =
    sql"""SELECT * FROM vehicle_photos WHERE vehicle_id IN (${VehiclesSql.id.values.list(ids)})"""
      .query(codec)

  val deleteByVehicleIdSql: Command[VehicleId] =
    sql"""DELETE FROM vehicle_photos WHERE vehicle_id = ${VehiclesSql.id}""".command
}
