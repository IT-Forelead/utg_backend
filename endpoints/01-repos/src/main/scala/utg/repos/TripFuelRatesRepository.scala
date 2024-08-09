package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.repos.sql.TripFuelRatesSql
import utg.repos.sql.dto

trait TripFuelRatesRepository[F[_]] {
  def create(input: dto.TripFuelRate): F[Unit]
}

object TripFuelRatesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripFuelRatesRepository[F] = new TripFuelRatesRepository[F] {
    override def create(input: dto.TripFuelRate): F[Unit] =
      TripFuelRatesSql.insert.execute(input)
  }
}
