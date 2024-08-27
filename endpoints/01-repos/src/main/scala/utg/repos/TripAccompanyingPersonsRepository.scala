package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripAccompanyingPersonsSql
import utg.repos.sql.dto

trait TripAccompanyingPersonsRepository[F[_]] {
  def create(inputList: NonEmptyList[dto.TripAccompanyingPerson]): F[Unit]
  def findByIds(
      ids: NonEmptyList[TripId]
    ): F[Map[TripId, List[dto.TripAccompanyingPerson]]]
  def deleteByTripId(tripId: TripId): F[Unit]
}

object TripAccompanyingPersonsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripAccompanyingPersonsRepository[F] = new TripAccompanyingPersonsRepository[F] {
    override def create(inputList: NonEmptyList[dto.TripAccompanyingPerson]): F[Unit] = {
      val list = inputList.toList
      TripAccompanyingPersonsSql.insert(list).execute(list)
    }

    override def findByIds(
        ids: NonEmptyList[TripId]
      ): F[Map[TripId, List[dto.TripAccompanyingPerson]]] = {
      val tripIds = ids.toList
      TripAccompanyingPersonsSql.findByTripIds(tripIds).queryList(tripIds).map {
        _.groupBy(_.tripId)
      }
    }

    override def deleteByTripId(tripId: TripId): F[Unit] =
      TripAccompanyingPersonsSql.deleteByTripId.execute(tripId)
  }
}
