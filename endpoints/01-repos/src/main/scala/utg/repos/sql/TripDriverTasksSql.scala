package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripDriverTaskId
import utg.domain.TripId

private[repos] object TripDriverTasksSql extends Sql[TripDriverTaskId] {
  private[repos] val codec: Codec[dto.TripDriverTask] =
    (id *: zonedDateTime *: TripsSql.id *: nes *: zonedDateTime *: nes *: nes *: nes *: nonNegInt.opt
      *: nonNegDouble.opt *: nonNegDouble.opt *: bool)
      .to[dto.TripDriverTask]

  val insert: Command[dto.TripDriverTask] =
    sql"""INSERT INTO trip_driver_tasks VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripDriverTask] =
    sql"""SELECT * FROM trip_driver_tasks
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)

  val update: Command[dto.TripDriverTask] =
    sql"""UPDATE trip_driver_tasks
       SET whose_discretion = $nes,
        arrival_time = $zonedDateTime,
        pickup_location = $nes,
        delivery_location = $nes,
        freight_name = $nes,
        number_of_interactions = ${nonNegInt.opt},
        distance = ${nonNegDouble.opt},
        freight_volume = ${nonNegDouble.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case tripDriverTask: dto.TripDriverTask =>
          tripDriverTask.whoseDiscretion *: tripDriverTask.arrivalTime *: tripDriverTask.pickupLocation *:
            tripDriverTask.deliveryLocation *: tripDriverTask.freightName *: tripDriverTask.numberOfInteractions *:
            tripDriverTask.distance *: tripDriverTask.freightVolume *: tripDriverTask.id *: EmptyTuple
      }

  val delete: Command[TripDriverTaskId] =
    sql"""UPDATE trip_driver_tasks SET deleted = true WHERE id = $id""".command

  val findById: Query[TripDriverTaskId, dto.TripDriverTask] =
    sql"""SELECT * FROM trip_driver_tasks WHERE id = $id LIMIT 1""".query(codec)
}
