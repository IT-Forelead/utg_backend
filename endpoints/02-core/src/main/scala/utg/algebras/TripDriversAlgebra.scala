package utg.algebras

import cats.MonadThrow
import cats.effect.std.Random
import org.typelevel.log4cats.Logger

import utg.domain.ResponseData
import utg.domain.TripDriver
import utg.domain.TripId
import utg.domain.args.tripDrivers.TripDriverFilters
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.TripDriversRepository

trait TripDriversAlgebra[F[_]] {
  def get(filters: TripDriverFilters): F[ResponseData[TripDriver]]
  def getByTripId(tripId: TripId): F[List[TripDriver]]
}

object TripDriversAlgebra {
  def make[F[_]: Calendar: GenUUID: Random](
      tripDriversRepository: TripDriversRepository[F]
    )(implicit
      F: MonadThrow[F],
      logger: Logger[F],
    ): TripDriversAlgebra[F] =
    new TripDriversAlgebra[F] {
      override def getByTripId(tripId: TripId): F[List[TripDriver]] =
        tripDriversRepository.getByTripId(tripId)

      override def get(filters: TripDriverFilters): F[ResponseData[TripDriver]] =
        tripDriversRepository.get(filters)
    }
}
