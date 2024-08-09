package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.repos.sql.TripGivenFuelsSql
import utg.repos.sql.dto

trait TripGivenFuelsRepository[F[_]] {
  def create(input: dto.TripGivenFuel): F[Unit]
}

object TripGivenFuelsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripGivenFuelsRepository[F] = new TripGivenFuelsRepository[F] {
    override def create(input: dto.TripGivenFuel): F[Unit] =
      TripGivenFuelsSql.insert.execute(input)
  }
}
