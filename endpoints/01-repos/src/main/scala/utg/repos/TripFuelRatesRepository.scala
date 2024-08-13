package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripFuelRatesSql
import utg.repos.sql.dto

trait TripFuelRatesRepository[F[_]] {
  def create(input: dto.TripFuelRate): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripFuelRate]]
}

object TripFuelRatesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripFuelRatesRepository[F] = new TripFuelRatesRepository[F] {
    override def create(input: dto.TripFuelRate): F[Unit] =
      TripFuelRatesSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripFuelRate]] =
      TripFuelRatesSql.selectByTripId.queryList(tripId)
  }
}
