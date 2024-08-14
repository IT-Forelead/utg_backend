package utg.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._

import utg.domain.ResponseData
import utg.domain.TripId
import utg.domain.args.trips._
import utg.exception.AError
import utg.repos.sql.TripsSql
import utg.repos.sql.dto

trait TripsRepository[F[_]] {
  def create(trip: dto.Trip): F[Unit]
  def get(filters: TripFilters): F[ResponseData[dto.Trip]]
  def findById(id: TripId): F[Option[dto.Trip]]
  def update(id: TripId)(update: dto.Trip => dto.Trip): F[Unit]
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

    override def update(id: TripId)(update: dto.Trip => dto.Trip): F[Unit] =
      OptionT(TripsSql.findById.queryOption(id)).cataF(
        AError.Internal(s"Trip not found by id [$id]").raiseError[F, Unit],
        trip => TripsSql.update.execute(update(trip)),
      )

    override def updateDoctorApproval(input: TripDoctorApprovalInput): F[Unit] =
      TripsSql.updateDoctorApprovalSql.execute(input)

    override def updateChiefMechanicApproval(input: TripChiefMechanicInput): F[Unit] =
      TripsSql.updateChiefMechanicApprovalSql.execute(input)
  }
}
