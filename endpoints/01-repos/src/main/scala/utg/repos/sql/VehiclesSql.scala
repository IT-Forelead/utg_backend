package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.codec.all.varchar
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters

private[repos] object VehiclesSql extends Sql[VehicleId] {
  private[repos] val codec: Codec[dto.Vehicle] =
    (id *: zonedDateTime *: BranchesSql.id *: VehicleCategoriesSql.id *: nes *: registeredNumber.opt *: inventoryNumber
      *: nonNegInt *: nes.opt *: nes.opt *: nes.opt *: conditionType *: fuelType.opt *: nes.opt *: gpsTrackerType.opt
      *: nonNegDouble.opt *: nonNegDouble.opt *: bool).to[dto.Vehicle]

  val insert: Command[dto.Vehicle] =
    sql"""INSERT INTO vehicles VALUES ($codec)""".command

  def get(filters: VehicleFilters): AppliedFragment = {
    val searchFilters = List(
      filters.brand.map(s => s"%$s%").map(sql"v.brand ILIKE $varchar"),
      filters.registeredNumber.map(sql"v.registered_number = $registeredNumber"),
      filters.conditionType.map(sql"v.condition = $conditionType"),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT
        v.id AS vehicle_id,
        v.created_at AS created_at,
        v.branch_id AS branch_id,
        v.vehicle_category_id AS vehicle_category_id,
        v.brand AS brand,
        v.registered_number AS registered_number,
        v.inventory_number AS inventory_number,
        v.year_of_release AS year_of_release,
        v.body_number AS body_number,
        v.chassis_number AS chassis_number,
        v.engine_number AS engine_number,
        v.condition AS condition,
        v.fuel_type AS fuel_type,
        v.description AS description,
        v.gps_tracker AS gps_tracker,
        v.fuel_level_sensor AS fuel_level_sensor,
        v.fuel_tank_volume AS fuel_tank_volume,
        v.deleted AS deleted,
        COUNT(*) OVER() AS total
        FROM vehicles v""".apply(Void)
    baseQuery.whereAndOpt(searchFilters)
  }
}
