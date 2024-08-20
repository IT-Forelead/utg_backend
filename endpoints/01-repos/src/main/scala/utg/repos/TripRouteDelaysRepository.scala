package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripRouteDelaysSql
import utg.repos.sql.dto

trait TripRouteDelaysRepository[F[_]] {
  def create(input: dto.TripRouteDelay): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripRouteDelay]]
}

object TripRouteDelaysRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripRouteDelaysRepository[F] = new TripRouteDelaysRepository[F] {
    override def create(input: dto.TripRouteDelay): F[Unit] =
      TripRouteDelaysSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripRouteDelay]] =
      TripRouteDelaysSql.selectByTripId.queryList(tripId)
  }
}
