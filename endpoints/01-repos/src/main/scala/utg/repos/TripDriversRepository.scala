package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._

import utg.domain.ResponseData
import utg.domain.TripDriver
import utg.domain.TripId
import utg.domain.args.tripDrivers.TripDriverFilters
import utg.domain.args.tripDrivers.UpdateDriverExamination
import utg.repos.sql.TripDriversSql
import utg.repos.sql.dto

trait TripDriversRepository[F[_]] {
  def create(inputList: NonEmptyList[dto.TripDriver]): F[Unit]
  def get(filters: TripDriverFilters): F[ResponseData[TripDriver]]
  def updateDriverExamination(input: UpdateDriverExamination): F[Unit]
  def getByTripId(tripId: TripId): F[List[TripDriver]]
  def findByTripIds(ids: NonEmptyList[TripId]): F[Map[TripId, List[TripDriver]]]
  def deleteByTripId(tripId: TripId): F[Unit]
}

object TripDriversRepository {
  def make[F[_]: Async](
      usersRepo: UsersRepository[F]
    )(implicit
      session: Resource[F, Session[F]]
    ): TripDriversRepository[F] = new TripDriversRepository[F] {
    private def makeTripDrivers(tripDriversDto: List[dto.TripDriver]): F[List[TripDriver]] = {
      val driverIds = tripDriversDto.map(_.driverId)
      val doctorIds = tripDriversDto.flatMap(_.doctorId)
      NonEmptyList
        .fromList(driverIds ++ doctorIds)
        .fold(List.empty[TripDriver].pure[F]) { userIds =>
          for {
            usersById <- usersRepo.findByIds(userIds)
            tripDrivers = tripDriversDto.map { tripDriverDto =>
              tripDriverDto.toDomain(
                driver = usersById.get(tripDriverDto.driverId),
                doctor = tripDriverDto.doctorId.flatMap(usersById.get),
              )
            }
          } yield tripDrivers
        }
    }

    override def create(inputList: NonEmptyList[dto.TripDriver]): F[Unit] = {
      val list = inputList.toList
      TripDriversSql.insert(list).execute(list)
    }

    override def updateDriverExamination(input: UpdateDriverExamination): F[Unit] =
      TripDriversSql.update.execute(input)

    override def get(filters: TripDriverFilters): F[ResponseData[TripDriver]] = {
      val af = TripDriversSql.get(filters).paginateOpt(filters.limit, filters.offset)
      af.fragment
        .query(TripDriversSql.codec *: int8)
        .queryList(af.argument)
        .flatMap { data =>
          val count = data.headOption.fold(0L)(_.tail.head)
          makeTripDrivers(data.map(_.head)).map { tripDrivers =>
            ResponseData(tripDrivers, count)
          }
        }
    }

    override def getByTripId(tripId: TripId): F[List[TripDriver]] =
      TripDriversSql
        .selectByTripId
        .queryList(tripId)
        .flatMap { tripDriversDto =>
          makeTripDrivers(tripDriversDto)
        }

    override def findByTripIds(
        ids: NonEmptyList[TripId]
      ): F[Map[TripId, List[TripDriver]]] = {
      val tripIds = ids.toList
      TripDriversSql
        .findByTripIds(tripIds)
        .queryList(tripIds)
        .flatMap { tripDriversDto =>
          makeTripDrivers(tripDriversDto).map(_.groupBy(_.tripId))
        }
    }

    override def deleteByTripId(tripId: TripId): F[Unit] =
      TripDriversSql.deleteByTripIdSql.execute(tripId)
  }
}
