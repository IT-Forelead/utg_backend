package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.implicits._
import eu.timepit.refined.types.numeric.NonNegDouble

import utg.domain.TripId
import utg.domain.TripVehicleIndicator
import utg.domain.TripVehicleIndicatorId
import utg.domain.args.tripVehicleIndicators.TripVehicleIndicatorInput
import utg.domain.enums.VehicleIndicatorActionType
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripVehicleIndicatorsRepository
import utg.repos.TripsRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripVehicleIndicatorsAlgebra[F[_]] {
  def create(input: TripVehicleIndicatorInput): F[TripVehicleIndicatorId]
  def getByTripId(tripId: TripId): F[List[TripVehicleIndicator]]
}

object TripVehicleIndicatorsAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripVehicleIndicatorsRepository: TripVehicleIndicatorsRepository[F],
      tripsRepository: TripsRepository[F],
    ): TripVehicleIndicatorsAlgebra[F] =
    new TripVehicleIndicatorsAlgebra[F] {
      override def create(input: TripVehicleIndicatorInput): F[TripVehicleIndicatorId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripVehicleIndicatorId],
          trip =>
            for {
              id <- ID.make[F, TripVehicleIndicatorId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripVehicleIndicator = dto.TripVehicleIndicator(
                id = id,
                createdAt = now,
                tripId = trip.id,
                vehicleId = trip.vehicleId,
                actionType = input.actionType,
                scheduledTime = input.scheduledTime,
                currentDateTime = input.currentDateTime,
                odometerIndicator = input.odometerIndicator,
                paidDistance = input.paidDistance.getOrElse(NonNegDouble.unsafeFrom(0d)),
              )
              _ <- tripVehicleIndicatorsRepository.create(dtoTripVehicleIndicator)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripVehicleIndicator]] =
        for {
          dtoVehicleIndicators <- tripVehicleIndicatorsRepository.getByTripId(tripId)
          isFulledEnteredAction = dtoVehicleIndicators.exists(
            _.actionType == VehicleIndicatorActionType.Enter
          )
          isFulledExitAction = dtoVehicleIndicators.exists(
            _.actionType == VehicleIndicatorActionType.Exit
          )
          tripVehicleIndicators = dtoVehicleIndicators.map { vi =>
            TripVehicleIndicator(
              id = vi.id,
              createdAt = vi.createdAt,
              tripId = vi.tripId,
              vehicleId = vi.vehicleId,
              actionType = vi.actionType,
              scheduledTime = vi.scheduledTime,
              currentDateTime = vi.currentDateTime,
              odometerIndicator = vi.odometerIndicator,
              paidDistance = vi.paidDistance,
              isFulledExitAction = isFulledExitAction,
              isFulledEnteredAction = isFulledEnteredAction,
            )
          }
        } yield tripVehicleIndicators
    }
}
