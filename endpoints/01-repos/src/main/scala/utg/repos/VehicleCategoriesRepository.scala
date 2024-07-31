package utg.repos

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.VehicleCategory
import utg.domain.VehicleCategoryId
import utg.domain.args.vehicleCategories.VehicleCategoryFilters
import utg.exception.AError
import utg.repos.sql.VehicleCategoriesSql
import utg.repos.sql.dto

trait VehicleCategoriesRepository[F[_]] {
  def create(branch: dto.VehicleCategory): F[Unit]
  def get(filters: VehicleCategoryFilters): F[List[dto.VehicleCategory]]
  def update(id: VehicleCategoryId)(update: dto.VehicleCategory => dto.VehicleCategory): F[Unit]
  def findByIds(ids: List[VehicleCategoryId]): F[Map[VehicleCategoryId, VehicleCategory]]
}

object VehicleCategoriesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): VehicleCategoriesRepository[F] = new VehicleCategoriesRepository[F] {
    override def create(vehicleCategory: dto.VehicleCategory): F[Unit] =
      VehicleCategoriesSql.insert.execute(vehicleCategory)

    override def get(filters: VehicleCategoryFilters): F[List[dto.VehicleCategory]] = {
      val af = VehicleCategoriesSql.get(filters)
      af.fragment
        .query(VehicleCategoriesSql.codec)
        .queryList(af.argument)
    }

    override def update(
        id: VehicleCategoryId
      )(
        update: dto.VehicleCategory => dto.VehicleCategory
      ): F[Unit] =
      OptionT(VehicleCategoriesSql.findById.queryOption(id)).cataF(
        AError.Internal(s"Vehicle category not found by id [$id]").raiseError[F, Unit],
        vc => VehicleCategoriesSql.update.execute(update(vc)),
      )

    override def findByIds(
        ids: List[VehicleCategoryId]
      ): F[Map[VehicleCategoryId, VehicleCategory]] =
      NonEmptyList.fromList(ids).fold(Map.empty[VehicleCategoryId, VehicleCategory].pure[F]) {
        rIds =>
          val vcIds = rIds.toList
          VehicleCategoriesSql.findByIds(vcIds).queryList(vcIds).map {
            _.map { dtoVehicleCategory =>
              dtoVehicleCategory.id -> VehicleCategory(
                dtoVehicleCategory.id,
                dtoVehicleCategory.name,
                dtoVehicleCategory.vehicleType,
              )
            }.toMap
          }
      }
  }
}
