package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripDriversSql
import utg.repos.sql.dto

trait TripDriversRepository[F[_]] {
  def create(inputList: NonEmptyList[dto.TripDriver]): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripDriver]]
  def findByTripIds(
      ids: NonEmptyList[TripId]
    ): F[Map[TripId, List[dto.TripDriver]]]
}

object TripDriversRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripDriversRepository[F] = new TripDriversRepository[F] {
    override def create(inputList: NonEmptyList[dto.TripDriver]): F[Unit] = {
      val list = inputList.toList
      TripDriversSql.insert(list).execute(list)
    }

    override def getByTripId(tripId: TripId): F[List[dto.TripDriver]] =
      TripDriversSql.selectByTripId.queryList(tripId)

    override def findByTripIds(
        ids: NonEmptyList[TripId]
      ): F[Map[TripId, List[dto.TripDriver]]] = {
      val tripIds = ids.toList
      TripDriversSql.findByTripIds(tripIds).queryList(tripIds).map {
        _.groupBy(_.tripId)
      }
    }
  }
}
