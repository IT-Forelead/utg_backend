package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.codec.all.date
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.TripId
import utg.domain.args.trips.TripDoctorApprovalInput
import utg.domain.args.trips.TripFilters

private[repos] object TripsSql extends Sql[TripId] {
  private[repos] val codec =
    (id *: zonedDateTime *: date *: date.opt *: nes *: nes.opt *: nes.opt *: nes.opt *: workingModeType
      *: nes.opt *: VehiclesSql.id *: UsersSql.id *: VehiclesSql.id.opt
      *: VehiclesSql
        .id
        .opt *: UsersSql.id.opt *: AssetsSql.id.opt *: nonNegDouble.opt *: UsersSql.id.opt
      *: AssetsSql.id.opt *: nes.opt *: bool).to[dto.Trip]

  val insert: Command[dto.Trip] =
    sql"""INSERT INTO trips VALUES ($codec)""".command

  val findById: Query[TripId, dto.Trip] =
    sql"""SELECT * FROM trips WHERE deleted = false AND id = $id""".query(codec)

  def get(filters: TripFilters): AppliedFragment = {
    val searchFilters = List(
      filters.workingMode.map(sql"working_mode = $workingModeType"),
      filters.vehicleId.map(sql"vehicle_id = ${VehiclesSql.id}"),
      filters.driverId.map(sql"driver_id = ${UsersSql.id}"),
      filters.startDate.map(sql"start_date = $date"),
      filters.endDate.map(sql"end_date = $date"),
      filters.from.map(sql"created_at >= $zonedDateTime"),
      filters.to.map(sql"created_at <= $zonedDateTime"),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT *, COUNT(*) OVER() AS total FROM trips WHERE deleted = false""".apply(Void)
    baseQuery.andOpt(searchFilters)
  }

  val updateDoctorApprovalSql: Command[TripDoctorApprovalInput] =
    sql"""UPDATE trips
       SET doctor_id = ${UsersSql.id.opt},
       doctor_signature = ${AssetsSql.id.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case tfe: TripDoctorApprovalInput =>
          tfe.doctorId *: tfe.doctorSignature *: tfe.tripId *: EmptyTuple
      }
}
