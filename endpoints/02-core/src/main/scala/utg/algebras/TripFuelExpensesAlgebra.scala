package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.implicits._

import utg.domain.TripFuelExpense
import utg.domain.TripFuelExpenseId
import utg.domain.TripId
import utg.domain.args.tripFuelExpenses.TripFuelExpenseInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripFuelExpensesRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripFuelExpensesAlgebra[F[_]] {
  def create(input: TripFuelExpenseInput): F[TripFuelExpenseId]
  def getByTripId(tripId: TripId): F[List[TripFuelExpense]]
}

object TripFuelExpensesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripFuelExpensesRepository: TripFuelExpensesRepository[F],
      usersRepository: UsersRepository[F],
      tripsRepository: TripsRepository[F],
    ): TripFuelExpensesAlgebra[F] =
    new TripFuelExpensesAlgebra[F] {
      override def create(input: TripFuelExpenseInput): F[TripFuelExpenseId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripFuelExpenseId],
          trip =>
            for {
              id <- ID.make[F, TripFuelExpenseId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripFuelExpense = dto.TripFuelExpense(
                id = id,
                createdAt = now,
                tripId = trip.id,
                vehicleId = trip.vehicleId,
                fuelBrand = input.fuelBrand,
                brandCode = input.brandCode,
                fuelGiven = input.fuelGiven,
                fuelAttendant = input.fuelAttendant,
                attendantSignature = input.attendantSignature,
                fuelInTank = input.fuelInTank,
                fuelRemaining = input.fuelRemaining,
                normChangeCoefficient = input.normChangeCoefficient,
                equipmentWorkingTime = input.equipmentWorkingTime,
                engineWorkingTime = input.engineWorkingTime,
                tankCheckMechanicId = input.tankCheckMechanicId,
                tankCheckMechanicSignature = input.tankCheckMechanicSignature,
                remainingCheckMechanicId = input.remainingCheckMechanicId,
                remainingCheckMechanicSignature = input.remainingCheckMechanicSignature,
                dispatcherId = input.dispatcherId,
                dispatcherSignature = input.dispatcherSignature,
              )
              _ <- tripFuelExpensesRepository.create(dtoTripFuelExpense)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripFuelExpense]] =
        for {
          dtoTripFuelExpenses <- tripFuelExpensesRepository.getByTripId(tripId)
          userIds = dtoTripFuelExpenses
            .flatMap(tfe =>
              List(
                tfe.tankCheckMechanicId,
                tfe.remainingCheckMechanicId,
                tfe.dispatcherId,
              ).flatten
            )
            .distinct
          users <- usersRepository.findByIds(userIds)
          tripFuelExpenses = dtoTripFuelExpenses.map { fe =>
            TripFuelExpense(
              id = fe.id,
              createdAt = fe.createdAt,
              tripId = fe.tripId,
              vehicleId = fe.vehicleId,
              fuelBrand = fe.fuelBrand,
              brandCode = fe.brandCode,
              fuelGiven = fe.fuelGiven,
              fuelAttendant = fe.fuelAttendant,
              attendantSignature = fe.attendantSignature,
              fuelInTank = fe.fuelInTank,
              fuelRemaining = fe.fuelRemaining,
              normChangeCoefficient = fe.normChangeCoefficient,
              equipmentWorkingTime = fe.equipmentWorkingTime,
              engineWorkingTime = fe.engineWorkingTime,
              tankCheckMechanic = fe.tankCheckMechanicId.flatMap(users.get),
              tankCheckMechanicSignature = fe.tankCheckMechanicSignature,
              remainingCheckMechanic = fe.remainingCheckMechanicId.flatMap(users.get),
              remainingCheckMechanicSignature = fe.remainingCheckMechanicSignature,
              dispatcher = fe.dispatcherId.flatMap(users.get),
              dispatcherSignature = fe.dispatcherSignature,
            )
          }
        } yield tripFuelExpenses
    }
}
