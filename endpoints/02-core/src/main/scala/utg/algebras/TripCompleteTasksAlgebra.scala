package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AuthedUser.User
import utg.domain.TripCompleteTask
import utg.domain.TripCompleteTaskId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.args.tripCompleteTasks.TripCompleteTaskInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripCompleteTasksRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripCompleteTasksAlgebra[F[_]] {
  def create(input: TripCompleteTaskInput, userId: UserId): F[TripCompleteTaskId]
  def getByTripId(tripId: TripId): F[List[TripCompleteTask]]
}

object TripCompleteTasksAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripCompleteTasksRepository: TripCompleteTasksRepository[F],
      tripsRepository: TripsRepository[F],
      usersRepository: UsersRepository[F],
    ): TripCompleteTasksAlgebra[F] =
    new TripCompleteTasksAlgebra[F] {
      override def create(input: TripCompleteTaskInput, userId: UserId): F[TripCompleteTaskId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripCompleteTaskId],
          trip =>
            for {
              id <- ID.make[F, TripCompleteTaskId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripCompleteTask = dto.TripCompleteTask(
                id = id,
                createdAt = now,
                tripId = trip.id,
                commuteNumber = input.commuteNumber,
                loadNumbers = input.loadNumbers,
                arrivalTime = input.arrivalTime,
                consignorFullName = input.consignorFullName,
                consignorSignature = input.consignorSignature,
                driverId = userId,
              )
              _ <- tripCompleteTasksRepository.create(dtoTripCompleteTask)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripCompleteTask]] =
        for {
          dtoTripCompleteTasks <- tripCompleteTasksRepository.getByTripId(tripId)
          drivers <- NonEmptyList
            .fromList(dtoTripCompleteTasks.map(_.driverId))
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          tripGivenFuels = dtoTripCompleteTasks.map(tct =>
            TripCompleteTask(
              id = tct.id,
              createdAt = tct.createdAt,
              tripId = tct.tripId,
              commuteNumber = tct.commuteNumber,
              loadNumbers = tct.loadNumbers,
              arrivalTime = tct.arrivalTime,
              consignorFullName = tct.consignorFullName,
              consignorSignature = tct.consignorSignature,
              driver = drivers.get(tct.driverId),
            )
          )
        } yield tripGivenFuels
    }
}
