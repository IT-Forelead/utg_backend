package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import utg.domain.{TripDriverTaskId, TripId}
import utg.domain.args.tripDriverTasks.TripDriverTaskFilters

private[repos] object TripDriverTasksSql extends Sql[TripDriverTaskId] {
  private[repos] val codec: Codec[dto.TripDriverTask] =
    (id *: zonedDateTime *: TripsSql.id *: nes *: zonedDateTime *: nes *: nes *: nes *: nonNegInt *: nonNegDouble *: nonNegDouble *: bool)
      .to[dto.TripDriverTask]

  val insert: Command[dto.TripDriverTask] =
    sql"""INSERT INTO trip_driver_tasks VALUES ($codec)""".command

  def get(filters: TripDriverTaskFilters): AppliedFragment = {
    val searchFilters = List(
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT * FROM trip_driver_tasks WHERE deleted = false""".apply(Void)
    baseQuery.andOpt(searchFilters)
  }

  val selectByTripId: Query[TripId, dto.TripDriverTask] =
    sql"""SELECT * FROM trip_driver_tasks
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)

  val update: Command[dto.TripDriverTask] =
    sql"""UPDATE trip_driver_tasks
       SET trip_id = ${TripsSql.id},
        whose_discretion = $nes,
        arrival_time = $zonedDateTime,
        pickup_location = $nes,
        delivery_location = $nes,
        freight_name = $nes,
        number_of_interactions = $nonNegInt,
        distance = $nonNegDouble,
        freight_volume = $nonNegDouble
       WHERE id = $id
     """
      .command
      .contramap {
        case tripDriverTask: dto.TripDriverTask =>
          tripDriverTask.tripId *: tripDriverTask.whoseDiscretion *: tripDriverTask.arrivalTime *: tripDriverTask.pickupLocation *: tripDriverTask.deliveryLocation *: tripDriverTask.freightName *: tripDriverTask.numberOfInteractions *: tripDriverTask.distance *: tripDriverTask.freightVolume *: tripDriverTask.id *: EmptyTuple
      }

  def delete: Command[TripDriverTaskId] =
    sql"""DELETE FROM trip_driver_tasks u WHERE u.id = $id""".command

  val findById: Query[TripDriverTaskId, dto.TripDriverTask] =
    sql"""SELECT * FROM trip_driver_tasks WHERE id = $id LIMIT 1""".query(codec)
}
