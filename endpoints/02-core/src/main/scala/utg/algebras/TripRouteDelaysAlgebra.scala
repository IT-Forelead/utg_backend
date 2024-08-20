package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AuthedUser.User
import utg.domain.TripId
import utg.domain.TripRouteDelay
import utg.domain.TripRouteDelayId
import utg.domain.UserId
import utg.domain.args.tripRouteDelays.TripRouteDelayInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripRouteDelaysRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripRouteDelaysAlgebra[F[_]] {
  def create(input: TripRouteDelayInput, userId: UserId): F[TripRouteDelayId]
  def getByTripId(tripId: TripId): F[List[TripRouteDelay]]
}

object TripRouteDelaysAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripRouteDelaysRepository: TripRouteDelaysRepository[F],
      tripsRepository: TripsRepository[F],
      usersRepository: UsersRepository[F],
    ): TripRouteDelaysAlgebra[F] =
    new TripRouteDelaysAlgebra[F] {
      override def create(input: TripRouteDelayInput, userId: UserId): F[TripRouteDelayId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripRouteDelayId],
          trip =>
            for {
              id <- ID.make[F, TripRouteDelayId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripRouteDelay = dto.TripRouteDelay(
                id = id,
                createdAt = now,
                tripId = trip.id,
                name = input.name,
                startTime = input.startTime,
                endTime = input.endTime,
                userId = userId,
                userSignature = input.userSignature,
              )
              _ <- tripRouteDelaysRepository.create(dtoTripRouteDelay)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripRouteDelay]] =
        for {
          dtoTripRouteDelays <- tripRouteDelaysRepository.getByTripId(tripId)
          users <- NonEmptyList
            .fromList(dtoTripRouteDelays.map(_.userId))
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          tripRouteDelays = dtoTripRouteDelays.map(trd =>
            TripRouteDelay(
              id = trd.id,
              createdAt = trd.createdAt,
              tripId = trd.tripId,
              name = trd.name,
              startTime = trd.startTime,
              endTime = trd.endTime,
              user = users.get(trd.userId),
              userSignature = trd.userSignature,
            )
          )
        } yield tripRouteDelays
    }
}
