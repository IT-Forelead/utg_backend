package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AuthedUser.User
import utg.domain.TripFuelSupply
import utg.domain.TripFuelSupplyId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.args.tripFuelSupplies.TripFuelSupplyInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripFuelSuppliesRepository
import utg.repos.TripFuelSupplyItemsRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripFuelSuppliesAlgebra[F[_]] {
  def create(input: TripFuelSupplyInput, dispatcherId: UserId): F[TripFuelSupplyId]
  def getByTripId(tripId: TripId): F[List[TripFuelSupply]]
}

object TripFuelSuppliesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripFuelSuppliesRepository: TripFuelSuppliesRepository[F],
      tripFuelSupplyItemsRepository: TripFuelSupplyItemsRepository[F],
      tripsRepository: TripsRepository[F],
      usersRepository: UsersRepository[F],
    ): TripFuelSuppliesAlgebra[F] =
    new TripFuelSuppliesAlgebra[F] {
      override def create(
          input: TripFuelSupplyInput,
          dispatcherId: UserId,
        ): F[TripFuelSupplyId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripFuelSupplyId],
          trip =>
            for {
              id <- ID.make[F, TripFuelSupplyId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripFuelInspection = dto.TripFuelSupply(
                id = id,
                createdAt = now,
                tripId = trip.id,
                vehicleId = trip.vehicleId,
                dispatcherId = dispatcherId,
                dispatcherSignature = input.dispatcherSignature,
              )
              _ <- tripFuelSuppliesRepository.create(dtoTripFuelInspection)
              _ <- tripFuelSupplyItemsRepository.create(id, input.fuelSupplies)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripFuelSupply]] =
        for {
          dtoTripFuelInspections <- tripFuelSuppliesRepository.getByTripId(tripId)
          dispatchers <- NonEmptyList
            .fromList(dtoTripFuelInspections.map(_.dispatcherId))
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          tripFuelSupplies <- dtoTripFuelInspections.traverse { tfi =>
            for {
              fuelSupplies <- tripFuelSupplyItemsRepository
                .getByTripFuelSupplyId(tfi.id)
                .map(_.map(_.toDomain))
              data = TripFuelSupply(
                id = tfi.id,
                createdAt = tfi.createdAt,
                tripId = tfi.tripId,
                vehicleId = tfi.vehicleId,
                fuelSupplies = fuelSupplies,
                dispatcher = dispatchers.get(tfi.dispatcherId),
                dispatcherSignature = tfi.dispatcherSignature,
              )
            } yield data
          }
        } yield tripFuelSupplies
    }
}
