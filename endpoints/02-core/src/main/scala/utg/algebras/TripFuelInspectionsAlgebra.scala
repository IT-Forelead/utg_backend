package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.implicits._

import utg.domain.TripFuelInspectionId
import utg.domain.UserId
import utg.domain.args.tripFuelExpenses._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripFuelInspectionsRepository
import utg.repos.TripsRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripFuelInspectionsAlgebra[F[_]] {
  def create(input: TripFuelInspectionInput, mechanicId: UserId): F[TripFuelInspectionId]
}

object TripFuelInspectionsAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripFuelInspectionsRepository: TripFuelInspectionsRepository[F],
      tripsRepository: TripsRepository[F],
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
                fuelInTank = input.fuelInTank,
                mechanicId = mechanicId,
                mechanicSignature = input.mechanicSignature,
              )
              _ <- tripFuelInspectionsRepository.create(dtoTripFuelInspection)
            } yield id,
        )
    }
}
