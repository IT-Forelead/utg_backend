package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.codec.all.date
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.TripId
import utg.domain.args.trips._

private[repos] object TripsSql extends Sql[TripId] {
  private[repos] val codec =
    (id *: zonedDateTime *: date *: date.opt *: nes.opt *: nes.opt *: nes.opt *: nes.opt *: workingModeType.opt
      *: nes.opt *: VehiclesSql.id *: nes.opt *: bool).to[dto.Trip]

  val insert: Command[dto.Trip] =
    sql"""INSERT INTO trips VALUES ($codec)""".command

  val findById: Query[TripId, dto.Trip] =
    sql"""SELECT * FROM trips WHERE deleted = false AND id = $id""".query(codec)

  def get(filters: TripFilters): AppliedFragment = {
    val searchFilters = List(
      filters.workingMode.map(sql"working_mode = $workingModeType"),
      filters.vehicleId.map(sql"vehicle_id = ${VehiclesSql.id}"),
      filters.startDate.map(sql"start_date = $date"),
      filters.endDate.map(sql"end_date = $date"),
      filters.from.map(sql"created_at >= $zonedDateTime"),
      filters.to.map(sql"created_at <= $zonedDateTime"),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT *, COUNT(*) OVER() AS total FROM trips WHERE deleted = false""".apply(Void)
    baseQuery.andOpt(searchFilters)
  }

  val update: Command[dto.Trip] =
    sql"""UPDATE trips
       SET start_date = $date,
       end_date = ${date.opt},
       serial_number = ${nes.opt},
       first_tab = ${nes.opt},
       second_tab = ${nes.opt},
       third_tab = ${nes.opt},
       work_order = ${workingModeType.opt},
       summation = ${nes.opt},
       vehicle_id = ${VehiclesSql.id},
       notes = ${nes.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case trip: dto.Trip =>
          trip.startDate *: trip.endDate *: trip.serialNumber *: trip.firstTab *: trip.secondTab *: trip.thirdTab *:
            trip.workingMode *: trip.summation *: trip.vehicleId *: trip.notes *: trip.id *: EmptyTuple
      }
}
