package utg.repos

import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._

import utg.domain.ResponseData
import utg.domain.args.trips.TripFilters
import utg.repos.sql.AccompanyingPersonsSql
import utg.repos.sql.TripsSql
import utg.repos.sql.dto

trait TripsRepository[F[_]] {
  def create(trip: dto.Trip): F[Unit]
  def createAccompanyingPersons(inputList: List[dto.AccompanyingPerson]): F[Unit]
  def get(filters: TripFilters): F[ResponseData[dto.Trip]]
}

object TripsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripsRepository[F] = new TripsRepository[F] {
    override def create(trip: dto.Trip): F[Unit] =
      TripsSql.insert.execute(trip)

    override def createAccompanyingPersons(
        inputList: List[dto.AccompanyingPerson]
      ): F[Unit] = AccompanyingPersonsSql.insert(inputList).execute(inputList)

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
  }
}
