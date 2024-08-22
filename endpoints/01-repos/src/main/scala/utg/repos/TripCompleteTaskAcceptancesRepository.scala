package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripCompleteTaskAcceptancesSql
import utg.repos.sql.dto

trait TripCompleteTaskAcceptancesRepository[F[_]] {
  def create(input: dto.TripCompleteTaskAcceptance): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripCompleteTaskAcceptance]]
}

object TripCompleteTaskAcceptancesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripCompleteTaskAcceptancesRepository[F] = new TripCompleteTaskAcceptancesRepository[F] {
    override def create(input: dto.TripCompleteTaskAcceptance): F[Unit] =
      TripCompleteTaskAcceptancesSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripCompleteTaskAcceptance]] =
      TripCompleteTaskAcceptancesSql.selectByTripId.queryList(tripId)
  }
}
