package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.VehicleCategoryId

private[repos] object VehicleCategoriesSql extends Sql[VehicleCategoryId] {
  private[repos] val codec = (id *: nes *: bool).to[dto.VehicleCategory]

  val insert: Command[dto.VehicleCategory] =
    sql"""INSERT INTO vehicle_categories VALUES ($codec)""".command

  val select: Query[Void, dto.VehicleCategory] =
    sql"""SELECT * FROM vehicle_categories WHERE deleted = false ORDER BY name ASC"""
      .query(codec)

  val findById: Query[VehicleCategoryId, dto.VehicleCategory] =
    sql"""SELECT * FROM vehicle_categories WHERE id = $id AND deleted = false LIMIT 1"""
      .query(codec)

  def findByIds(ids: List[VehicleCategoryId]): Query[ids.type, dto.VehicleCategory] =
    sql"""SELECT * FROM vehicle_categories WHERE id IN (${id.values.list(ids)})""".query(codec)

  val update: Command[dto.VehicleCategory] =
    sql"""UPDATE vehicle_categories SET name = $nes WHERE id = $id"""
      .command
      .contramap {
        case vc: dto.VehicleCategory =>
          vc.name *: vc.id *: EmptyTuple
      }
}
