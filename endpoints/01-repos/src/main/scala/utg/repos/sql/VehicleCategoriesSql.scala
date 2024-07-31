package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.codec.all.varchar
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.VehicleCategoryId
import utg.domain.args.vehicleCategories.VehicleCategoryFilters

private[repos] object VehicleCategoriesSql extends Sql[VehicleCategoryId] {
  private[repos] val codec = (id *: nes *: vehicleType *: bool).to[dto.VehicleCategory]

  val insert: Command[dto.VehicleCategory] =
    sql"""INSERT INTO vehicle_categories VALUES ($codec)""".command

  val findById: Query[VehicleCategoryId, dto.VehicleCategory] =
    sql"""SELECT * FROM vehicle_categories WHERE id = $id AND deleted = false LIMIT 1"""
      .query(codec)

  def get(filters: VehicleCategoryFilters): AppliedFragment = {
    val searchFilters = List(
      filters.name.map(s => s"%$s%").map(sql"name ILIKE $varchar"),
      filters.vehicleType.map(sql"vehicle_type = $vehicleType"),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT * FROM vehicle_categories WHERE deleted = false""".apply(Void)
    baseQuery.andOpt(searchFilters)
  }

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
