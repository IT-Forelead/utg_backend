package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripTrailersSql
import utg.repos.sql.dto

trait TripTrailersRepository[F[_]] {
  def create(inputList: NonEmptyList[dto.TripTrailer]): F[Unit]
  def findByIds(
      ids: NonEmptyList[TripId]
    ): F[Map[TripId, List[dto.TripTrailer]]]
  def deleteByTripId(tripId: TripId): F[Unit]
}

object TripTrailersRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripTrailersRepository[F] = new TripTrailersRepository[F] {
    override def create(inputList: NonEmptyList[dto.TripTrailer]): F[Unit] = {
      val list = inputList.toList
      TripTrailersSql.insert(list).execute(list)
    }

    override def findByIds(
        ids: NonEmptyList[TripId]
      ): F[Map[TripId, List[dto.TripTrailer]]] = {
      val tripIds = ids.toList
      TripTrailersSql.findByTripIds(tripIds).queryList(tripIds).map {
        _.groupBy(_.tripId)
      }
    }

    override def deleteByTripId(tripId: TripId): F[Unit] =
      TripTrailersSql.deleteByTripId.execute(tripId)
  }
}
