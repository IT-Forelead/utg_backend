package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.std.Random
import cats.implicits._
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt

import utg.domain.TripDriverTask
import utg.domain.TripDriverTaskId
import utg.domain.TripId
import utg.domain.args.tripDriverTasks.TripDriverTaskInput
import utg.domain.args.tripDriverTasks.UpdateTripDriverTaskInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripDriverTasksRepository
import utg.repos.TripsRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripDriverTasksAlgebra[F[_]] {
  def getByTripId(tripId: TripId): F[List[TripDriverTask]]
  def findById(id: TripDriverTaskId): F[Option[TripDriverTask]]
  def create(tripDriverTaskInput: TripDriverTaskInput): F[TripDriverTaskId]
  def update(input: UpdateTripDriverTaskInput): F[Unit]
  def delete(id: TripDriverTaskId): F[Unit]
}
object TripDriverTasksAlgebra {
  def make[F[_]: Calendar: GenUUID: Random](
      tripDriverTasksRepository: TripDriverTasksRepository[F],
      tripsRepository: TripsRepository[F],
    )(implicit
      F: MonadThrow[F],
      P: PasswordHasher[F, SCrypt],
      logger: Logger[F],
    ): TripDriverTasksAlgebra[F] =
    new TripDriverTasksAlgebra[F] {
      override def getByTripId(tripId: TripId): F[List[TripDriverTask]] =
        for {
          dtoTripDriverTasks <- tripDriverTasksRepository.getByTripId(tripId)
          tripDriverTasks = dtoTripDriverTasks.map(tdt =>
            TripDriverTask(
              id = tdt.id,
              createdAt = tdt.createdAt,
              tripId = tdt.tripId,
              whoseDiscretion = tdt.whoseDiscretion,
              arrivalTime = tdt.arrivalTime,
              pickupLocation = tdt.pickupLocation,
              deliveryLocation = tdt.deliveryLocation,
              freightName = tdt.freightName,
              numberOfInteractions = tdt.numberOfInteractions,
              distance = tdt.distance,
              freightVolume = tdt.freightVolume,
            )
          )
        } yield tripDriverTasks

      override def findById(id: TripDriverTaskId): F[Option[TripDriverTask]] =
        tripDriverTasksRepository.findById(id)

      override def create(tripDriverTaskInput: TripDriverTaskInput): F[TripDriverTaskId] =
        OptionT(tripsRepository.findById(tripDriverTaskInput.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [${tripDriverTaskInput.tripId}]")
            .raiseError[F, TripDriverTaskId],
          trip =>
            for {
              id <- ID.make[F, TripDriverTaskId]
              now <- Calendar[F].currentZonedDateTime
              tripDriverTask = dto.TripDriverTask(
                id = id,
                createdAt = now,
                tripId = trip.id,
                whoseDiscretion = tripDriverTaskInput.whoseDiscretion,
                arrivalTime = tripDriverTaskInput.arrivalTime,
                pickupLocation = tripDriverTaskInput.pickupLocation,
                deliveryLocation = tripDriverTaskInput.deliveryLocation,
                freightName = tripDriverTaskInput.freightName,
                numberOfInteractions = tripDriverTaskInput.numberOfInteractions,
                distance = tripDriverTaskInput.distance,
                freightVolume = tripDriverTaskInput.freightVolume,
              )
              _ <- tripDriverTasksRepository.create(tripDriverTask)
            } yield id,
        )

      override def update(input: UpdateTripDriverTaskInput): F[Unit] =
        for {
          _ <- tripDriverTasksRepository.update(input.id)(tripDriverTask =>
            tripDriverTask.copy(
              whoseDiscretion = input.whoseDiscretion.getOrElse(tripDriverTask.whoseDiscretion),
              arrivalTime = input.arrivalTime,
              pickupLocation = input.pickupLocation.getOrElse(tripDriverTask.pickupLocation),
              deliveryLocation = input.deliveryLocation.getOrElse(tripDriverTask.deliveryLocation),
              freightName = input.freightName.getOrElse(tripDriverTask.freightName),
              numberOfInteractions = input.numberOfInteractions,
              distance = input.distance,
              freightVolume = input.freightVolume,
            )
          )
        } yield {}

      override def delete(id: TripDriverTaskId): F[Unit] =
        tripDriverTasksRepository.delete(id)
    }
}
