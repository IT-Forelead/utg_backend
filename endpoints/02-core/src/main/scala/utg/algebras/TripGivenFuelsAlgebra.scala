package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AuthedUser.User
import utg.domain.TripGivenFuel
import utg.domain.TripGivenFuelId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.args.tripFuelExpenses._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripGivenFuelsRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripGivenFuelsAlgebra[F[_]] {
  def create(input: TripGivenFuelInput, refuelerId: UserId): F[TripGivenFuelId]
  def getByTripId(tripId: TripId): F[List[TripGivenFuel]]
}

object TripGivenFuelsAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripGivenFuelsRepository: TripGivenFuelsRepository[F],
      tripsRepository: TripsRepository[F],
      usersRepository: UsersRepository[F],
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
                refuelerSignature = input.refuelerSignature,
              )
              _ <- tripGivenFuelsRepository.create(dtoTripGivenFuel)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripGivenFuel]] =
        for {
          dtoTripGivenFuels <- tripGivenFuelsRepository.getByTripId(tripId)
          refuelers <- NonEmptyList
            .fromList(dtoTripGivenFuels.map(_.refuelerId))
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          tripGivenFuels = dtoTripGivenFuels.map(tgf =>
            TripGivenFuel(
              id = tgf.id,
              createdAt = tgf.createdAt,
              tripId = tgf.tripId,
              vehicleId = tgf.vehicleId,
              fuelBrand = tgf.fuelBrand,
              brandCode = tgf.brandCode,
              fuelGiven = tgf.fuelGiven,
              refueler = refuelers.get(tgf.refuelerId),
              refuelerSignature = tgf.refuelerSignature,
            )
          )
        } yield tripGivenFuels
    }
}
