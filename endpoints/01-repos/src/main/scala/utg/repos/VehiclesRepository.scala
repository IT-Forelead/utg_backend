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

trait VehiclesRepository[F[_]] {
  def create(vehicle: Vehicle): F[Unit]
  def get(filters: VehicleFilters): F[ResponseData[Vehicle]]
}

object VehiclesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): VehiclesRepository[F] = new VehiclesRepository[F] {
    override def create(vehicle: Vehicle): F[Unit] =
      VehiclesSql.insert.execute(vehicle)

    override def get(filters: VehicleFilters): F[ResponseData[Vehicle]] = {
      val af = VehiclesSql
        .select(filters)
        .paginateOpt(filters.limit.map(_.value), filters.offset.map(_.value))
      af.fragment.query(VehiclesSql.codec *: int8).queryList(af.argument).flatMap { vehiclesDto =>
        ResponseData(vehiclesDto.map(_.head), vehiclesDto.headOption.fold(0L)(_.tail.head)).pure[F]
      }
    }
  }
}
