package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripFuelSuppliesSql
import utg.repos.sql.dto

trait TripFuelSuppliesRepository[F[_]] {
  def create(input: dto.TripFuelSupply): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripFuelSupply]]
}

object TripFuelSuppliesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripFuelSuppliesRepository[F] = new TripFuelSuppliesRepository[F] {
    override def create(input: dto.TripFuelSupply): F[Unit] =
      TripFuelSuppliesSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripFuelSupply]] =
      TripFuelSuppliesSql.selectByTripId.queryList(tripId)
  }
}
