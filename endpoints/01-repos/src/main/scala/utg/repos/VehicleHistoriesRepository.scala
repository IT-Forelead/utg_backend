package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import utg.domain.BranchId
import utg.domain.RegionId
import utg.domain.ResponseData
import utg.domain.VehicleCategory
import utg.domain.VehicleHistory
import utg.domain.VehicleHistoryId
import utg.domain.args.vehicleHistories.VehicleHistoryFilters
import utg.repos.sql.BranchesSql
import utg.repos.sql.RegionsSql
import utg.repos.sql.VehicleCategoriesSql
import utg.repos.sql.VehicleHistoriesSql
import utg.repos.sql.VehiclesSql
import utg.repos.sql.dto
import utg.repos.sql.dto.VehicleHistoryWithCategory

trait VehicleHistoriesRepository[F[_]] {
  def create(vehicle: dto.VehicleHistory): F[Unit]
  def get(filters: VehicleHistoryFilters): F[ResponseData[VehicleHistory]]
  def getAsStream(filters: VehicleHistoryFilters): fs2.Stream[F, dto.VehicleHistory]
  def makeVehicleHistory(vehicleDto: dto.VehicleHistory): F[VehicleHistory]
}

object VehicleHistoriesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): VehicleHistoriesRepository[F] = new VehicleHistoriesRepository[F] {
    override def create(vehicleHistory: dto.VehicleHistory): F[Unit] =
      VehicleHistoriesSql.insert.execute(vehicleHistory)

    def makeVehicleHistory(vehicleHistoryDto: dto.VehicleHistory): F[VehicleHistory] =
      for {
        vehicleCategory <-
          for {
            vehicle <- VehiclesSql.findById.queryList(vehicleHistoryDto.vehicleId)
            vehicleCategory <- VehicleCategoriesSql
              .findById
              .queryList(vehicle.head.vehicleCategoryId)
          } yield vehicleCategory.head.toDomain
        branch <-
          for {
            branch <- BranchesSql.findById.queryList(vehicleHistoryDto.branchId)
            region <- RegionsSql.findById.queryList(branch.head.regionId)
          } yield branch.head.toDomain(region.head.toDomain.some)
      } yield vehicleHistoryDto.toDomain(vehicleCategory, branch.some)

    private def makeVehicleHistories(dtos: List[dto.VehicleHistory]): F[List[VehicleHistory]] = {
      val vehicleHistoryIds = NonEmptyList.fromList(dtos.map(_.id))
      for {
        vh <- vehicleHistoryIds.fold(
          Map.empty[VehicleHistoryId, VehicleHistoryWithCategory].pure[F]
        ) { vhIds =>
          val rIds = vhIds.toList
          VehicleHistoriesSql
            .findByIds(rIds)
            .queryList(rIds)
            .map(_.map(r => r.id -> r).toMap)
        }
        ids = NonEmptyList.fromList(dtos.map(_.branchId))
        branchById <- ids.fold(Map.empty[BranchId, dto.Branch].pure[F]) { branches =>
          val branchesList = branches.toList
          BranchesSql
            .findByIds(branchesList)
            .queryList(branchesList)
            .map(_.map(b => b.id -> b).toMap)
        }
        maybeRegionIds = NonEmptyList.fromList(branchById.values.toList.map(_.regionId))
        regionById <- maybeRegionIds.fold(Map.empty[RegionId, dto.Region].pure[F]) { regionIds =>
          val regionIdList = regionIds.toList
          RegionsSql
            .findByIds(regionIdList)
            .queryList(regionIdList)
            .map(_.map(r => r.id -> r).toMap)
        }
      } yield dtos.flatMap { vehicleHistoryDto =>
        val vehicleCategoryList = vh.get(vehicleHistoryDto.id)
        vehicleCategoryList.map { vehicleCategory =>
          val maybeBranch = branchById
            .get(vehicleHistoryDto.branchId)
            .map(b => b.toDomain(regionById.get(b.regionId).map(_.toDomain)))

          vehicleHistoryDto.toDomain(
            VehicleCategory(
              id = vehicleCategory.vehicleCategoryId,
              name = vehicleCategory.vehicleCategoryName,
              vehicleType = vehicleCategory.vehicleCategoryType,
            ),
            maybeBranch,
          )
        }
      }
    }

    override def get(filters: VehicleHistoryFilters): F[ResponseData[VehicleHistory]] = {
      val af =
        VehicleHistoriesSql.get(filters).paginateOpt(filters.limit, filters.page)
      af.fragment
        .query(VehicleHistoriesSql.codec *: int8)
        .queryList(af.argument)
        .flatMap { data =>
          val count = data.headOption.fold(0L)(_.tail.head)
          makeVehicleHistories(data.map(_.head)).map { vehicleHistories =>
            ResponseData(vehicleHistories, count)
          }
        }
    }

    override def getAsStream(filters: VehicleHistoryFilters): fs2.Stream[F, dto.VehicleHistory] = {
      val af =
        VehicleHistoriesSql.get(filters).paginateOpt(filters.limit, filters.page)
      af.fragment.query(VehicleHistoriesSql.codec *: int8).queryStream(af.argument).map(_._1)
    }
  }
}
