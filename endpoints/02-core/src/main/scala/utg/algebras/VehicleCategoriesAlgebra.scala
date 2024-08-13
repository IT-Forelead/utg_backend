package utg.algebras

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import utg.domain.VehicleCategory
import utg.domain.VehicleCategoryId
import utg.domain.args.vehicleCategories._
import utg.effects.GenUUID
import utg.repos.VehicleCategoriesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait VehicleCategoriesAlgebra[F[_]] {
  def create(input: VehicleCategoryInput): F[VehicleCategoryId]
  def get(filters: VehicleCategoryFilters): F[List[VehicleCategory]]
  def update(input: VehicleCategory): F[Unit]
  def getVehicleCategories: F[List[VehicleCategory]]
}

object VehicleCategoriesAlgebra {
  def make[F[_]: MonadThrow: GenUUID](
      vehicleCategoriesRepository: VehicleCategoriesRepository[F]
    ): VehicleCategoriesAlgebra[F] =
    new VehicleCategoriesAlgebra[F] {
      override def create(input: VehicleCategoryInput): F[VehicleCategoryId] =
        for {
          id <- ID.make[F, VehicleCategoryId]
          dtoBranch = dto.VehicleCategory(
            id = id,
            name = input.name,
            vehicleType = input.vehicleType,
          )
          _ <- vehicleCategoriesRepository.create(dtoBranch)
        } yield id

      override def get(filters: VehicleCategoryFilters): F[List[VehicleCategory]] =
        vehicleCategoriesRepository
          .get(filters)
          .map(
            _.map(vc =>
              VehicleCategory(
                id = vc.id,
                name = vc.name,
                vehicleType = vc.vehicleType,
              )
            )
          )

      override def update(input: VehicleCategory): F[Unit] =
        vehicleCategoriesRepository.update(input.id)(_.copy(name = input.name))

      override def getVehicleCategories: F[List[VehicleCategory]] =
        for {
          vehicleCategories <- vehicleCategoriesRepository.getVehicleCategories
          vc = vehicleCategories.map { vehicleCategory =>
            vehicleCategory.toDomain
          }
        } yield vc
    }
}
