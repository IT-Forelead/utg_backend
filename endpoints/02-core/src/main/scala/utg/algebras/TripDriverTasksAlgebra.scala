package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.std.Random
import cats.implicits._
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt

import utg.domain.ResponseData
import utg.domain.TripDriverTask
import utg.domain.TripDriverTaskId
import utg.domain.TripId
import utg.domain.args.tripDriverTasks.TripDriverTaskFilters
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
  def get(filters: TripDriverTaskFilters): F[ResponseData[TripDriverTask]]
  def getByTripId(tripId: TripId): F[List[TripDriverTask]]
  def getAsStream(filters: TripDriverTaskFilters): F[fs2.Stream[F, TripDriverTask]]
  def findById(id: TripDriverTaskId): F[Option[TripDriverTask]]
  def create(tripDriverTaskInput: TripDriverTaskInput): F[TripDriverTaskId]
  def update(
      id: TripDriverTaskId,
      tripDriverTaskInput: UpdateTripDriverTaskInput,
    ): F[Unit]
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
      override def get(filters: TripDriverTaskFilters): F[ResponseData[TripDriverTask]] =
        tripDriverTasksRepository.get(filters)

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

      override def update(
          id: TripDriverTaskId,
          tripDriverTaskInput: UpdateTripDriverTaskInput,
        ): F[Unit] =
        for {
          _ <- tripDriverTasksRepository.update(id)(
            _.copy(
              whoseDiscretion = tripDriverTaskInput.whoseDiscretion,
              arrivalTime = tripDriverTaskInput.arrivalTime,
              pickupLocation = tripDriverTaskInput.pickupLocation,
              deliveryLocation = tripDriverTaskInput.deliveryLocation,
              freightName = tripDriverTaskInput.freightName,
              numberOfInteractions = tripDriverTaskInput.numberOfInteractions,
              distance = tripDriverTaskInput.distance,
              freightVolume = tripDriverTaskInput.freightVolume,
            )
          )
        } yield {}

      override def delete(id: TripDriverTaskId): F[Unit] =
        tripDriverTasksRepository.delete(id)

      override def getAsStream(filters: TripDriverTaskFilters): F[fs2.Stream[F, TripDriverTask]] =
        F.pure {
          tripDriverTasksRepository.getAsStream(filters).evalMap { tripDriverTask =>
            F.pure {
              tripDriverTasksRepository.makeTripDriverTask(tripDriverTask)
            }
          }
        }
    }
}
