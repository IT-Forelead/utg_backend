package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.VehicleId
import utg.domain.VehicleLicensePhotoId

private[repos] object VehicleLicensePhotosSql extends Sql[VehicleLicensePhotoId] {
  private[repos] val codec: Codec[dto.VehicleLicensePhoto] =
    (id *: VehiclesSql.id *: AssetsSql.id *: bool).to[dto.VehicleLicensePhoto]

  val insert: Command[dto.VehicleLicensePhoto] =
    sql"""INSERT INTO vehicle_license_photos VALUES ($codec)""".command

  val selectByVehicleId: Query[VehicleId, dto.VehicleLicensePhoto] =
    sql"""SELECT * FROM vehicle_license_photos WHERE deleted = false AND vehicle_id = ${VehiclesSql.id}"""
      .query(codec)

  def findByVehicleIds(ids: List[VehicleId]): Query[ids.type, dto.VehicleLicensePhoto] =
    sql"""SELECT * FROM vehicle_license_photos WHERE vehicle_id IN (${VehiclesSql.id.values.list(ids)})"""
      .query(codec)

  val deleteByVehicleIdSql: Command[VehicleId] =
    sql"""DELETE FROM vehicle_license_photos WHERE vehicle_id = ${VehiclesSql.id}""".command
}
