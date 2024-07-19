package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._

import utg.domain.ResponseData
import utg.domain.Vehicle
import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters
import utg.repos.sql.VehiclesSql
import utg.repos.sql.dto

trait VehiclesRepository[F[_]] {
  def create(vehicle: dto.Vehicle): F[Unit]
  def get(filters: VehicleFilters): F[ResponseData[dto.Vehicle]]
  def findByIds(ids: List[VehicleId]): F[Map[VehicleId, Vehicle]]
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

    override def findByIds(
        ids: List[VehicleId]
      ): F[Map[VehicleId, Vehicle]] =
      NonEmptyList.fromList(ids).fold(Map.empty[VehicleId, Vehicle].pure[F]) { rIds =>
        val branchIds = rIds.toList
        VehiclesSql.findByIds(branchIds).queryList(branchIds).map {
          _.map { dto =>
            dto.id -> Vehicle(
              id = dto.id,
              createdAt = dto.createdAt,
              vehicleType = dto.vehicleType,
              branch = None,
              vehicleCategory = None,
              brand = dto.brand,
              registeredNumber = dto.registeredNumber,
              inventoryNumber = dto.inventoryNumber,
              yearOfRelease = dto.yearOfRelease,
              bodyNumber = dto.bodyNumber,
              chassisNumber = dto.chassisNumber,
              engineNumber = dto.engineNumber,
              conditionType = dto.conditionType,
              fuelType = dto.fuelType,
              description = dto.description,
              gpsTracking = dto.gpsTracking,
              fuelLevelSensor = dto.fuelLevelSensor,
              fuelTankVolume = dto.fuelTankVolume,
            )
          }.toMap
        }
      }
  }
}
