package utg.repos

import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.domain.ResponseData
import utg.domain.Vehicle
import utg.domain.args.vehicles.VehicleFilters
import utg.repos.sql.VehiclesSql
import utg.repos.sql.dto

trait VehiclesRepository[F[_]] {
  def create(vehicle: dto.Vehicle): F[Unit]
  def get(filters: VehicleFilters): F[ResponseData[dto.Vehicle]]
}

object VehiclesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): VehiclesRepository[F] = new VehiclesRepository[F] {
    override def create(vehicle: dto.Vehicle): F[Unit] =
      VehiclesSql.insert.execute(vehicle)

    override def get(filters: VehicleFilters): F[ResponseData[dto.Vehicle]] = {
      val af = VehiclesSql.get(filters).paginateOpt(filters.limit, filters.offset)
      af.fragment
        .query(VehiclesSql.codec *: int8)
        .queryList(af.argument)
        .map { data =>
          val list = data.map(_.head)
          val count = data.headOption.fold(0L)(_.tail.head)
          ResponseData(list, count)
        }
    }
  }
}
