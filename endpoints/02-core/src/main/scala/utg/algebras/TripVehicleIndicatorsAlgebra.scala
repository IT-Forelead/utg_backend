package utg.algebras

import cats.MonadThrow
import cats.implicits._

import utg.domain.TripVehicleIndicatorId
import utg.domain.args.tripVehicleIndicators.TripVehicleIndicatorInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.TripVehicleIndicatorsRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripVehicleIndicatorsAlgebra[F[_]] {
  def create(input: TripVehicleIndicatorInput): F[TripVehicleIndicatorId]
}

object TripVehicleIndicatorsAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripVehicleIndicatorsRepository: TripVehicleIndicatorsRepository[F]
    ): TripVehicleIndicatorsAlgebra[F] =
    new TripVehicleIndicatorsAlgebra[F] {
      override def create(input: TripVehicleIndicatorInput): F[TripVehicleIndicatorId] =
        for {
          id <- ID.make[F, TripVehicleIndicatorId]
          now <- Calendar[F].currentZonedDateTime
          dtoTripVehicleIndicator = dto.TripVehicleIndicator(
            id = id,
            tripId = input.tripId,
            vehicleId = input.vehicleId,
            createdAt = now,
            indicator_type = input.indicatorType,
            registeredAt = input.registeredAt,
            paidDistance = input.paidDistance,
            odometerIndicator = input.odometerIndicator,
            currentDateTime = input.currentDateTime,
          )
          _ <- tripVehicleIndicatorsRepository.create(dtoTripVehicleIndicator)
        } yield id
    }
}
