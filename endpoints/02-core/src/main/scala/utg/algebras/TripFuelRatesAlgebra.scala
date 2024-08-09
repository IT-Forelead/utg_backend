package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.implicits._

import utg.domain.TripFuelRateId
import utg.domain.UserId
import utg.domain.args.tripFuelExpenses._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripFuelRatesRepository
import utg.repos.TripsRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripFuelRatesAlgebra[F[_]] {
  def create(input: TripFuelRateInput, dispatcherId: UserId): F[TripFuelRateId]
}

object TripFuelRatesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripFuelRatesRepository: TripFuelRatesRepository[F],
      tripsRepository: TripsRepository[F],
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
                normChangeCoefficient = input.normChangeCoefficient,
                equipmentWorkingTime = input.equipmentWorkingTime,
                engineWorkingTime = input.engineWorkingTime,
                dispatcherId = dispatcherId,
                dispatcherSignature = input.dispatcherSignature,
              )
              _ <- tripFuelRatesRepository.create(dtoTripFuelRate)
            } yield id,
        )
    }
}
