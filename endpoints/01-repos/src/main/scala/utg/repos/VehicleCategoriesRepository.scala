package utg.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.BranchId
import utg.domain.VehicleCategoryId
import utg.exception.AError
import utg.repos.sql.BranchesSql
import utg.repos.sql.VehicleCategoriesSql
import utg.repos.sql.dto

trait VehicleCategoriesRepository[F[_]] {
  def create(branch: dto.VehicleCategory): F[Unit]
  def get: F[List[dto.VehicleCategory]]
  def update(id: VehicleCategoryId)(update: dto.VehicleCategory => dto.VehicleCategory): F[Unit]
}

object VehicleCategoriesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): VehicleCategoriesRepository[F] = new VehicleCategoriesRepository[F] {
    override def create(vehicleCategory: dto.VehicleCategory): F[Unit] =
      VehicleCategoriesSql.insert.execute(vehicleCategory)

    override def get: F[List[dto.VehicleCategory]] =
      VehicleCategoriesSql.select.queryList(Void)

    override def update(
        id: VehicleCategoryId
      )(
        update: dto.VehicleCategory => dto.VehicleCategory
      ): F[Unit] =
      OptionT(VehicleCategoriesSql.findById.queryOption(id)).cataF(
        AError.Internal(s"Vehicle category not found by id [$id]").raiseError[F, Unit],
        vc => VehicleCategoriesSql.update.execute(update(vc)),
      )
  }
}
