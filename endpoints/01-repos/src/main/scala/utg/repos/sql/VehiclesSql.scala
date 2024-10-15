package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.codec.all.date
import skunk.codec.all.varchar
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters

private[repos] object VehiclesSql extends Sql[VehicleId] {
  private[repos] val codec: Codec[dto.Vehicle] =
    (id *: zonedDateTime *: vehicleType *: nes.opt *: nes *: nes.opt *: nes.opt *: nes.opt *: date.opt *: nes.opt
      *: nonNegInt.opt *: nonNegInt *: VehicleCategoriesSql.id *: nes.opt *: nes.opt *: nonNegInt *: nonNegInt
      *: nes.opt *: nonNegInt.opt *: nonNegInt *: nonNegInt *: nes.opt *: nes.opt *: BranchesSql.id *: inventoryNumber
      *: conditionType *: gpsTrackingType.opt *: nonNegDouble.opt *: nes.opt *: bool)
      .to[dto.Vehicle]

  val insert: Command[dto.Vehicle] =
    sql"""INSERT INTO vehicles VALUES ($codec)""".command

  val findById: Query[VehicleId, dto.Vehicle] =
    sql"""SELECT * FROM vehicles WHERE id = $id LIMIT 1""".query(codec)

  def findByIds(ids: List[VehicleId]): Query[ids.type, dto.Vehicle] =
    sql"""SELECT * FROM vehicles WHERE id IN (${id.values.list(ids)})""".query(codec)

  def get(filters: VehicleFilters): AppliedFragment = {
    val searchFilters = List(
      filters.brand.map(s => s"%$s%").map(sql"brand ILIKE $varchar"),
      filters.registeredNumber.map(sql"registered_number = $registeredNumber"),
      filters.conditionType.map(sql"condition = $conditionType"),
      filters.vehicleType.map(sql"vehicle_type = $vehicleType"),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT *, COUNT(*) OVER() AS total FROM vehicles""".apply(Void)
    baseQuery.whereAndOpt(searchFilters)
  }

  val update: Command[dto.Vehicle] =
    sql"""UPDATE vehicles
       SET vehicle_type = $vehicleType,
       registered_number = ${nes.opt},
       brand = $nes,
       color = ${nes.opt},
       owner = ${nes.opt},
       address = ${nes.opt},
       date_of_issue = ${date.opt},
       issuing_authority = ${nes.opt},
       pin = ${nonNegInt.opt},
       year_of_release = $nonNegInt,
       vehicle_category_id = ${VehicleCategoriesSql.id},
       body_number = ${nes.opt},
       chassis_number = ${nes.opt},
       max_mass = $nonNegInt,
       unload_mass = $nonNegInt,
       engine_number = ${nes.opt},
       engine_capacity = ${nonNegInt.opt},
       number_of_seats = $nonNegInt,
       number_of_standing_places = $nonNegInt,
       special_marks = ${nes.opt},
       license_number = ${nes.opt},
       branch_id = ${BranchesSql.id},
       condition = $conditionType,
       gps_tracking = ${gpsTrackingType.opt},
       fuel_level_sensor = ${nonNegDouble.opt},
       description = ${nes.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case vehicle: dto.Vehicle =>
          vehicle.vehicleType *: vehicle.registeredNumber *: vehicle.brand *: vehicle.color *: vehicle.owner *:
            vehicle.address *: vehicle.dateOfIssue *: vehicle.issuingAuthority *: vehicle.pin *:
            vehicle.yearOfRelease *: vehicle.vehicleCategoryId *: vehicle.bodyNumber *: vehicle.chassisNumber *:
            vehicle.maxMass *: vehicle.unloadMass *: vehicle.engineNumber *: vehicle.engineCapacity *:
            vehicle.numberOfSeats *: vehicle.numberOfStandingPlaces *: vehicle.specialMarks *: vehicle.licenseNumber *:
            vehicle.branchId *: vehicle.conditionType *: vehicle.gpsTracking *: vehicle.fuelLevelSensor *:
            vehicle.description *: vehicle.id *: EmptyTuple
      }
}
