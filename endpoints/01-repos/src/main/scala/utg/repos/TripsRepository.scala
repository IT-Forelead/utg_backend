package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._

import utg.domain.ResponseData
import utg.domain.TripId
import utg.domain.args.trips._
import utg.repos.sql.AccompanyingPersonsSql
import utg.repos.sql.TripsSql
import utg.repos.sql.dto

trait TripsRepository[F[_]] {
  def create(trip: dto.Trip): F[Unit]
  def createAccompanyingPersons(inputList: List[dto.AccompanyingPerson]): F[Unit]
  def get(filters: TripFilters): F[ResponseData[dto.Trip]]
  def findById(id: TripId): F[Option[dto.Trip]]
  def findAccompanyingPersonByIds(
      ids: List[TripId]
    ): F[Map[TripId, List[dto.AccompanyingPerson]]]
  def updateDoctorApproval(input: TripDoctorApprovalInput): F[Unit]
  def updateChiefMechanicApproval(input: TripChiefMechanicInput): F[Unit]
}

object TripsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripsRepository[F] = new TripsRepository[F] {
    override def create(trip: dto.Trip): F[Unit] =
      TripsSql.insert.execute(trip)

    override def createAccompanyingPersons(inputList: List[dto.AccompanyingPerson]): F[Unit] =
      AccompanyingPersonsSql.insert(inputList).execute(inputList)

    override def get(filters: TripFilters): F[ResponseData[dto.Trip]] = {
      val af = TripsSql.get(filters).paginateOpt(filters.limit, filters.offset)
      af.fragment
        .query(TripsSql.codec *: int8)
        .queryList(af.argument)
        .map { data =>
          val list = data.map(_.head)
          val count = data.headOption.fold(0L)(_.tail.head)
          ResponseData(list, count)
        }
    }

    override def findById(id: TripId): F[Option[dto.Trip]] =
      TripsSql.findById.queryOption(id)

    override def findAccompanyingPersonByIds(
        ids: List[TripId]
      ): F[Map[TripId, List[dto.AccompanyingPerson]]] =
      NonEmptyList
        .fromList(ids)
        .fold(Map.empty[TripId, List[dto.AccompanyingPerson]].pure[F]) { tIds =>
          val tripIds = tIds.toList
          AccompanyingPersonsSql.findByTripIds(tripIds).queryList(tripIds).map {
            _.groupBy(_.tripId)
          }
        }
    override def updateDoctorApproval(input: TripDoctorApprovalInput): F[Unit] =
      TripsSql.updateDoctorApprovalSql.execute(input)

    override def updateChiefMechanicApproval(input: TripChiefMechanicInput): F[Unit] =
      TripsSql.updateChiefMechanicApprovalSql.execute(input)
  }
}
