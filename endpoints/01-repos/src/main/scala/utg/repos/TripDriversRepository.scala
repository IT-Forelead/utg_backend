package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripDriver
import utg.domain.TripId
import utg.repos.sql.TripDriversSql
import utg.repos.sql.dto

trait TripDriversRepository[F[_]] {
  def create(inputList: NonEmptyList[dto.TripDriver]): F[Unit]
  def getByTripId(tripId: TripId): F[List[TripDriver]]
  def findByTripIds(
      ids: NonEmptyList[TripId]
    ): F[Map[TripId, List[TripDriver]]]
}

object TripDriversRepository {
  def make[F[_]: Async](
      usersRepo: UsersRepository[F]
    )(implicit
      session: Resource[F, Session[F]]
    ): TripDriversRepository[F] = new TripDriversRepository[F] {
    override def create(inputList: NonEmptyList[dto.TripDriver]): F[Unit] = {
      val list = inputList.toList
      TripDriversSql.insert(list).execute(list)
    }

    override def getByTripId(tripId: TripId): F[List[TripDriver]] =
      TripDriversSql
        .selectByTripId
        .queryList(tripId)
        .flatMap { tripDriversDto =>
          val driverIds = NonEmptyList.fromList(tripDriversDto.map(_.driverId))
          driverIds.fold(tripDriversDto.map(_.toDomain(None)).pure[F]) { userIds =>
            usersRepo.findByIds(userIds).map { driverById =>
              tripDriversDto.map { tripDriverDto =>
                tripDriverDto.toDomain(driverById.get(tripDriverDto.driverId))
              }
            }
          }
        }

    override def findByTripIds(
        ids: NonEmptyList[TripId]
      ): F[Map[TripId, List[TripDriver]]] = {
      val tripIds = ids.toList
      TripDriversSql.findByTripIds(tripIds).queryList(tripIds).flatMap { tripDriversDto =>
        val driverIds = NonEmptyList.fromList(tripDriversDto.map(_.driverId))
        driverIds
          .fold(tripDriversDto.map(_.toDomain(None)).pure[F]) { userIds =>
            usersRepo.findByIds(userIds).map { driverById =>
              tripDriversDto.map { tripDriverDto =>
                tripDriverDto.toDomain(driverById.get(tripDriverDto.driverId))
              }
            }
          }
          .map(_.groupBy(_.tripId))
      }
    }
  }
}
