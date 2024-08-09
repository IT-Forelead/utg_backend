package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.implicits._

import utg.domain.TripGivenFuelId
import utg.domain.UserId
import utg.domain.args.tripFuelExpenses._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripGivenFuelsRepository
import utg.repos.TripsRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripGivenFuelsAlgebra[F[_]] {
  def create(input: TripGivenFuelInput, refuelerId: UserId): F[TripGivenFuelId]
}

object TripGivenFuelsAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripGivenFuelsRepository: TripGivenFuelsRepository[F],
      tripsRepository: TripsRepository[F],
    ): TripGivenFuelsAlgebra[F] =
    new TripGivenFuelsAlgebra[F] {
      override def create(input: TripGivenFuelInput, refuelerId: UserId): F[TripGivenFuelId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripGivenFuelId],
          trip =>
            for {
              id <- ID.make[F, TripGivenFuelId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripGivenFuel = dto.TripGivenFuel(
                id = id,
                createdAt = now,
                tripId = trip.id,
                vehicleId = trip.vehicleId,
                fuelBrand = input.fuelBrand,
                brandCode = input.brandCode,
                fuelGiven = input.fuelGiven,
                refuelerId = refuelerId,
                attendantSignature = input.attendantSignature,
              )
              _ <- tripGivenFuelsRepository.create(dtoTripGivenFuel)
            } yield id,
        )
    }
}
