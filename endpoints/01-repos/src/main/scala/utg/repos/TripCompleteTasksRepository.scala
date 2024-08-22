package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripCompleteTasksSql
import utg.repos.sql.dto

trait TripCompleteTasksRepository[F[_]] {
  def create(input: dto.TripCompleteTask): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripCompleteTask]]
}

object TripCompleteTasksRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripCompleteTasksRepository[F] = new TripCompleteTasksRepository[F] {
    override def create(input: dto.TripCompleteTask): F[Unit] =
      TripCompleteTasksSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripCompleteTask]] =
      TripCompleteTasksSql.selectByTripId.queryList(tripId)
  }
}
