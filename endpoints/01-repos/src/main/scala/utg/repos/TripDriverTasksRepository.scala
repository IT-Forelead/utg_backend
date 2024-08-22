package utg.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import skunk.Session
import uz.scala.skunk.syntax.all._

import utg.domain.TripDriverTask
import utg.domain.TripDriverTaskId
import utg.domain.TripId
import utg.exception.AError
import utg.repos.sql.TripDriverTasksSql
import utg.repos.sql.dto

trait TripDriverTasksRepository[F[_]] {
  def findById(id: TripDriverTaskId): F[Option[TripDriverTask]]
  def create(tripDriverTaskAndHash: dto.TripDriverTask): F[Unit]
  def update(id: TripDriverTaskId)(update: dto.TripDriverTask => dto.TripDriverTask): F[Unit]
  def delete(id: TripDriverTaskId): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripDriverTask]]
}

object TripDriverTasksRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripDriverTasksRepository[F] = new TripDriverTasksRepository[F] {
    override def findById(id: TripDriverTaskId): F[Option[TripDriverTask]] =
      OptionT(TripDriverTasksSql.findById.queryOption(id)).map(_.toDomain).value

    override def create(tripDriverTask: dto.TripDriverTask): F[Unit] =
      TripDriverTasksSql.insert.execute(tripDriverTask)

    override def getByTripId(tripId: TripId): F[List[dto.TripDriverTask]] =
      TripDriverTasksSql.selectByTripId.queryList(tripId)

    override def update(
        id: TripDriverTaskId
      )(
        update: dto.TripDriverTask => dto.TripDriverTask
      ): F[Unit] =
      OptionT(TripDriverTasksSql.findById.queryOption(id)).cataF(
        AError.Internal(s"TripDriverTask not found by id [$id]").raiseError[F, Unit],
        tripDriverTask => TripDriverTasksSql.update.execute(update(tripDriverTask)),
      )

    override def delete(id: TripDriverTaskId): F[Unit] =
      TripDriverTasksSql.delete.execute(id)
  }
}
