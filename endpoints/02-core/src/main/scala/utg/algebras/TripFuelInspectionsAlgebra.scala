package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AuthedUser.User
import utg.domain.TripFuelInspection
import utg.domain.TripFuelInspectionId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.args.tripFuelExpenses._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripFuelInspectionItemsRepository
import utg.repos.TripFuelInspectionsRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripFuelInspectionsAlgebra[F[_]] {
  def create(input: TripFuelInspectionInput, mechanicId: UserId): F[TripFuelInspectionId]
  def getByTripId(tripId: TripId): F[List[TripFuelInspection]]
}

object TripFuelInspectionsAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripFuelInspectionsRepository: TripFuelInspectionsRepository[F],
      tripFuelInspectionItemsRepository: TripFuelInspectionItemsRepository[F],
      tripsRepository: TripsRepository[F],
      usersRepository: UsersRepository[F],
    ): TripFuelInspectionsAlgebra[F] =
    new TripFuelInspectionsAlgebra[F] {
      override def create(
          input: TripFuelInspectionInput,
          mechanicId: UserId,
        ): F[TripFuelInspectionId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripFuelInspectionId],
          trip =>
            for {
              id <- ID.make[F, TripFuelInspectionId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripFuelInspection = dto.TripFuelInspection(
                id = id,
                createdAt = now,
                tripId = trip.id,
                vehicleId = trip.vehicleId,
                actionType = input.actionType,
//                fuelInTank = input.fuelInTank,
                mechanicId = mechanicId,
                mechanicSignature = input.mechanicSignature,
              )
              _ <- tripFuelInspectionsRepository.create(dtoTripFuelInspection)
              _ <- tripFuelInspectionItemsRepository.create(id, input.fuels)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripFuelInspection]] =
        for {
          dtoTripFuelInspections <- tripFuelInspectionsRepository.getByTripId(tripId)
          mechanics <- NonEmptyList
            .fromList(dtoTripFuelInspections.map(_.mechanicId))
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          tripFuelInspections <- dtoTripFuelInspections.traverse { tfi =>
            for {
              fuels <- tripFuelInspectionItemsRepository
                .getByTripFuelInspectionId(tfi.id)
                .map(_.map(_.toDomain))
              data = TripFuelInspection(
                id = tfi.id,
                createdAt = tfi.createdAt,
                tripId = tfi.tripId,
                vehicleId = tfi.vehicleId,
                actionType = tfi.actionType,
                fuels = fuels,
                mechanic = mechanics.get(tfi.mechanicId),
                mechanicSignature = tfi.mechanicSignature,
              )
            } yield data
          }
        } yield tripFuelInspections
    }
}
