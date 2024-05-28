package utg.repos.sql

import skunk._
import skunk.codec.all.varchar
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.Vehicle
import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters

private[repos] object VehiclesSql extends Sql[VehicleId] {
  private[repos] val codec: Codec[Vehicle] =
    (id *: zonedDateTime *: nes *: registeredNumber *: vehicleType *: nonNegDouble).to[Vehicle]

  val insert: Command[Vehicle] =
    sql"""INSERT INTO vehicles VALUES ($codec)""".command

  private def searchFilter(filters: VehicleFilters): List[Option[AppliedFragment]] =
    List(
      filters.name.map(s => s"%$s%").map(sql"v.name ILIKE $varchar"),
      filters.registeredNumber.map(sql"v.registered_number = $registeredNumber"),
      filters.vehicleType.map(sql"v.vehicle_type = $vehicleType"),
    )

  def select(filters: VehicleFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT
              v.id AS vehicle_id,
              v.created_at AS created_at,
              v.name AS firstname,
              v.registered_number AS lastname,
              v.vehicle_type AS login,
              v.fuel_tank_volume AS phone,
              COUNT(*) OVER() AS total
          FROM vehicles v""".stripMargin
    baseQuery(Void).whereAndOpt(searchFilter(filters))
  }
}
