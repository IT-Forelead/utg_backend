package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripGivenFuelsSql
import utg.repos.sql.dto

trait TripGivenFuelsRepository[F[_]] {
  def create(input: dto.TripGivenFuel): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripGivenFuel]]
}

object TripGivenFuelsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripGivenFuelsRepository[F] = new TripGivenFuelsRepository[F] {
    override def create(input: dto.TripGivenFuel): F[Unit] =
      TripGivenFuelsSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripGivenFuel]] =
      TripGivenFuelsSql.selectByTripId.queryList(tripId)
  }
}
