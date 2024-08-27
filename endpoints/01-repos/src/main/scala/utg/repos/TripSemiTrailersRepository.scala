package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripSemiTrailersSql
import utg.repos.sql.dto

trait TripSemiTrailersRepository[F[_]] {
  def create(inputList: NonEmptyList[dto.TripSemiTrailer]): F[Unit]
  def findByIds(
      ids: NonEmptyList[TripId]
    ): F[Map[TripId, List[dto.TripSemiTrailer]]]
  def deleteByTripId(tripId: TripId): F[Unit]
}

object TripSemiTrailersRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripSemiTrailersRepository[F] = new TripSemiTrailersRepository[F] {
    override def create(inputList: NonEmptyList[dto.TripSemiTrailer]): F[Unit] = {
      val list = inputList.toList
      TripSemiTrailersSql.insert(list).execute(list)
    }

    override def findByIds(
        ids: NonEmptyList[TripId]
      ): F[Map[TripId, List[dto.TripSemiTrailer]]] = {
      val tripIds = ids.toList
      TripSemiTrailersSql.findByTripIds(tripIds).queryList(tripIds).map {
        _.groupBy(_.tripId)
      }
    }

    override def deleteByTripId(tripId: TripId): F[Unit] =
      TripSemiTrailersSql.deleteByTripId.execute(tripId)
  }
}
