package utg.repos

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._

import utg.domain.BranchId
import utg.domain.RegionId
import utg.domain.ResponseData
import utg.domain.Vehicle
import utg.domain.VehicleCategory
import utg.domain.VehicleCategoryId
import utg.domain.VehicleFuelItem
import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters
import utg.exception.AError
import utg.repos.sql.BranchesSql
import utg.repos.sql.RegionsSql
import utg.repos.sql.VehicleCategoriesSql
import utg.repos.sql.VehicleFuelItemsSql
import utg.repos.sql.VehiclesSql
import utg.repos.sql.dto

trait VehiclesRepository[F[_]] {
  def create(vehicle: dto.Vehicle): F[Unit]
  def get(filters: VehicleFilters): F[ResponseData[Vehicle]]
  def getAsStream(filters: VehicleFilters): fs2.Stream[F, dto.Vehicle]
  def makeVehicle(vehicleDto: dto.Vehicle): F[Vehicle]
  def findByIds(ids: List[VehicleId]): F[Map[VehicleId, Vehicle]]
  def update(id: VehicleId)(update: dto.Vehicle => dto.Vehicle): F[Unit]
}

object VehiclesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): VehiclesRepository[F] = new VehiclesRepository[F] {
    override def create(vehicle: dto.Vehicle): F[Unit] =
      VehiclesSql.insert.execute(vehicle)

    def makeVehicle(vehicleDto: dto.Vehicle): F[Vehicle] =
      for {
        vehicleCategory <- VehicleCategoriesSql
          .findById
          .queryList(vehicleDto.vehicleCategoryId)
          .map { vehicleCategory =>
            VehicleCategory(
              id = vehicleDto.vehicleCategoryId,
              name = vehicleCategory.head.name,
              vehicleType = vehicleCategory.head.vehicleType,
            )
          }
        fuels <- VehicleFuelItemsSql
          .selectByVehicleId
          .queryList(vehicleDto.id)
          .map(_.map(_.toDomain))
        branch <-
          (for {
            branch <- OptionT(BranchesSql.findById.queryOption(vehicleDto.branchId))
            region <- OptionT(RegionsSql.findById.queryOption(branch.regionId))
          } yield branch.toDomain(region.toDomain.some)).value
      } yield vehicleDto.toDomain(branch, vehicleCategory, fuels)

    private def makeVehicles(dtos: List[dto.Vehicle]): F[List[Vehicle]] = {
      val vehicleCategoryIds = NonEmptyList.fromList(dtos.map(_.vehicleCategoryId))
      for {
        vehicleCategories <- vehicleCategoryIds.fold(
          Map.empty[VehicleCategoryId, VehicleCategory].pure[F]
        ) { vehicleCategoryIds =>
          val vcIds = vehicleCategoryIds.toList
          VehicleCategoriesSql
            .findByIds(vcIds)
            .queryList(vcIds)
            .map {
              _.map { dtoVehicleCategory =>
                dtoVehicleCategory.id -> VehicleCategory(
                  dtoVehicleCategory.id,
                  dtoVehicleCategory.name,
                  dtoVehicleCategory.vehicleType,
                )
              }.toMap
            }
        }
        branchIds = NonEmptyList.fromList(dtos.map(_.branchId))
        branchById <- branchIds.fold(Map.empty[BranchId, dto.Branch].pure[F]) { branches =>
          val branchesList = branches.toList
          BranchesSql
            .findByIds(branchesList)
            .queryList(branchesList)
            .map(_.map(b => b.id -> b).toMap)
        }
        vehicleIds = NonEmptyList.fromList(dtos.map(_.id))
        fuelsByVehicleId <- vehicleIds.fold(Map.empty[VehicleId, List[VehicleFuelItem]].pure[F]) {
          vIds =>
            val vehicleIdsList = vIds.toList
            VehicleFuelItemsSql
              .findByVehicleIds(vehicleIdsList)
              .queryList(vehicleIdsList)
              .map(_.map(_.toDomain).groupBy(_.vehicleId))
        }
        maybeRegionIds = NonEmptyList.fromList(branchById.values.toList.map(_.regionId))
        regionById <- maybeRegionIds.fold(Map.empty[RegionId, dto.Region].pure[F]) { regionIds =>
          val regionIdList = regionIds.toList
          RegionsSql
            .findByIds(regionIdList)
            .queryList(regionIdList)
            .map(_.map(r => r.id -> r).toMap)
        }
      } yield dtos.flatMap { vehicleDto =>
        val vehicleCategoryList = vehicleCategories.get(vehicleDto.vehicleCategoryId)
        vehicleCategoryList.map { vehicleCategory =>
          val maybeBranch = branchById
            .get(vehicleDto.branchId)
            .map(b => b.toDomain(regionById.get(b.regionId).map(_.toDomain)))
          vehicleDto.toDomain(
            maybeBranch,
            VehicleCategory(
              id = vehicleDto.vehicleCategoryId,
              name = vehicleCategory.name,
              vehicleType = vehicleCategory.vehicleType,
            ),
            fuels = fuelsByVehicleId.getOrElse(vehicleDto.id, Nil),
          )
        }
      }
    }

    override def get(filters: VehicleFilters): F[ResponseData[Vehicle]] = {
      val af =
        VehiclesSql.get(filters).paginateOpt(filters.limit, filters.page)
      af.fragment
        .query(VehiclesSql.codec *: int8)
        .queryList(af.argument)
        .flatMap { data =>
          val count = data.headOption.fold(0L)(_.tail.head)
          makeVehicles(data.map(_.head)).map { vehicles =>
            ResponseData(vehicles, count)
          }
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
              description = dto.description,
              gpsTracking = dto.gpsTracking,
              fuelLevelSensor = dto.fuelLevelSensor,
              fuels = Nil,
            )
          }.toMap
        }
      }

    override def getAsStream(filters: VehicleFilters): fs2.Stream[F, dto.Vehicle] = {
      val af =
        VehiclesSql.get(filters).paginateOpt(filters.limit, filters.page)
      af.fragment.query(VehiclesSql.codec *: int8).queryStream(af.argument).map(_._1)
    }

    override def update(id: VehicleId)(update: dto.Vehicle => dto.Vehicle): F[Unit] =
      OptionT(VehiclesSql.findById.queryOption(id)).cataF(
        AError.Internal(s"Vehicle not found by id [$id]").raiseError[F, Unit],
        vehicle => VehiclesSql.update.execute(update(vehicle)),
      )
  }
}
