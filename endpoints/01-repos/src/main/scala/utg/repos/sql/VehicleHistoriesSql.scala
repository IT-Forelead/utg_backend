package utg.repos.sql

import skunk._
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.VehicleHistoryId
import utg.domain.args.vehicleHistories.VehicleHistoryFilters
import utg.repos.sql.dto.VehicleHistoryWithCategory

private[repos] object VehicleHistoriesSql extends Sql[VehicleHistoryId] {
  private[repos] val codec: Codec[dto.VehicleHistory] =
    (id *: zonedDateTime *: VehiclesSql.id *: BranchesSql.id *: registeredNumber.opt)
      .to[dto.VehicleHistory]

  private[repos] val vehicleHistoryWithCategory: Codec[dto.VehicleHistoryWithCategory] =
    (id *: zonedDateTime *: VehicleCategoriesSql.id *: nes *: vehicleType *: registeredNumber.opt)
      .to[dto.VehicleHistoryWithCategory]

  val insert: Command[dto.VehicleHistory] =
    sql"""INSERT INTO vehicle_histories VALUES ($codec)""".command

  val findById: Query[VehicleHistoryId, dto.VehicleHistory] =
    sql"""SELECT * FROM vehicle_histories WHERE id = $id LIMIT 1"""
      .query(codec)

  def findByIds(ids: List[VehicleHistoryId]): Query[ids.type, VehicleHistoryWithCategory] =
    sql"""SELECT vh.id, vh.created_at, vc.id, vc.name, vc.vehicle_type, vh.registered_number FROM vehicle_histories vh
         LEFT JOIN vehicles v on vh.vehicle_id = v.id
         LEFT JOIN vehicle_categories vc on v.vehicle_category_id = vc.id
         WHERE vh.id IN (${id.values.list(ids)})""".query(vehicleHistoryWithCategory)

  def get(filters: VehicleHistoryFilters): AppliedFragment = {
    val searchFilters = List(
      filters.registeredNumber.map(sql"v.registered_number = $registeredNumber")
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT
        v.id AS vehicle_id,
        v.created_at AS created_at,
        v.vehicle_id AS vehicle_id,
        v.branch_id AS branch_id,
        v.registered_number AS registered_number,
        COUNT(*) OVER() AS total
        FROM vehicle_histories v""".apply(Void)
    baseQuery.whereAndOpt(searchFilters)
  }
}
