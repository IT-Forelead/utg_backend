package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AuthedUser.User
import utg.domain.TripFuelRate
import utg.domain.TripFuelRateId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.args.tripFuelExpenses._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripFuelRatesRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripFuelRatesAlgebra[F[_]] {
  def create(input: TripFuelRateInput, dispatcherId: UserId): F[TripFuelRateId]
  def getByTripId(tripId: TripId): F[List[TripFuelRate]]
}

object TripFuelRatesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripFuelRatesRepository: TripFuelRatesRepository[F],
      tripsRepository: TripsRepository[F],
      usersRepository: UsersRepository[F],
    ): TripFuelRatesAlgebra[F] =
    new TripFuelRatesAlgebra[F] {
      override def create(input: TripFuelRateInput, dispatcherId: UserId): F[TripFuelRateId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripFuelRateId],
          trip =>
            for {
              id <- ID.make[F, TripFuelRateId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripFuelRate = dto.TripFuelRate(
                id = id,
                createdAt = now,
                tripId = trip.id,
                fuelType = input.fuelType,
                normChangeCoefficient = input.normChangeCoefficient,
                equipmentWorkingTime = input.equipmentWorkingTime,
                engineWorkingTime = input.engineWorkingTime,
                dispatcherId = dispatcherId,
                dispatcherSignature = input.dispatcherSignature,
              )
              _ <- tripFuelRatesRepository.create(dtoTripFuelRate)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripFuelRate]] =
        for {
          dtoTripFuelRates <- tripFuelRatesRepository.getByTripId(tripId)
          dispatchers <- NonEmptyList
            .fromList(dtoTripFuelRates.map(_.dispatcherId))
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          tripFuelRates = dtoTripFuelRates.map(tfr =>
            TripFuelRate(
              id = tfr.id,
              createdAt = tfr.createdAt,
              tripId = tfr.tripId,
              fuelType = tfr.fuelType,
              normChangeCoefficient = tfr.normChangeCoefficient,
              equipmentWorkingTime = tfr.equipmentWorkingTime,
              engineWorkingTime = tfr.engineWorkingTime,
              dispatcher = dispatchers.get(tfr.dispatcherId),
              dispatcherSignature = tfr.dispatcherSignature,
            )
          )
        } yield tripFuelRates
    }
}
