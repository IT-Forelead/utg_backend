package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.implicits._

import utg.domain.TripId
import utg.domain.TripVehicleAcceptance
import utg.domain.TripVehicleAcceptanceId
import utg.domain.args.tripVehicleAcceptances.TripVehicleAcceptanceInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripVehicleAcceptancesRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripVehicleAcceptancesAlgebra[F[_]] {
  def create(input: TripVehicleAcceptanceInput): F[TripVehicleAcceptanceId]
  def getByTripId(tripId: TripId): F[List[TripVehicleAcceptance]]
}

object TripVehicleAcceptancesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripVehicleAcceptancesRepository: TripVehicleAcceptancesRepository[F],
      usersRepository: UsersRepository[F],
      tripsRepository: TripsRepository[F],
    ): TripVehicleAcceptancesAlgebra[F] =
    new TripVehicleAcceptancesAlgebra[F] {
      override def create(input: TripVehicleAcceptanceInput): F[TripVehicleAcceptanceId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripVehicleAcceptanceId],
          trip =>
            for {
              id <- ID.make[F, TripVehicleAcceptanceId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripVehicleAcceptance = dto.TripVehicleAcceptance(
                id = id,
                createdAt = now,
                tripId = trip.id,
                vehicleId = trip.vehicleId,
                actionType = input.actionType,
                conditionType = input.conditionType,
                mechanicId = input.mechanicId,
                mechanicSignature = input.mechanicSignature,
                driverId = trip.driverId,
                driverSignature = input.driverSignature,
              )
              _ <- tripVehicleAcceptancesRepository.create(dtoTripVehicleAcceptance)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripVehicleAcceptance]] =
        for {
          dtoTripVehicleAcceptances <- tripVehicleAcceptancesRepository.getByTripId(tripId)
          userIds = dtoTripVehicleAcceptances
            .flatMap(tva => tva.mechanicId ++ tva.driverId.some)
            .distinct
          users <- usersRepository.findByIds(userIds)
          tripFuelExpenses = dtoTripVehicleAcceptances.map { fe =>
            TripVehicleAcceptance(
              id = fe.id,
              createdAt = fe.createdAt,
              tripId = fe.tripId,
              vehicleId = fe.vehicleId,
              actionType = fe.actionType,
              conditionType = fe.conditionType,
              mechanic = fe.mechanicId.flatMap(users.get),
              mechanicSignature = fe.mechanicSignature,
              driver = users.get(fe.driverId),
              driverSignature = fe.driverSignature,
            )
          }
        } yield tripFuelExpenses
    }
}
