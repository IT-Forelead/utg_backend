package utg.algebras

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import eu.timepit.refined.types.string.NonEmptyString

import utg.domain.VehicleCategory
import utg.domain.VehicleCategoryId
import utg.effects.GenUUID
import utg.repos.VehicleCategoriesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait VehicleCategoriesAlgebra[F[_]] {
  def create(name: NonEmptyString): F[VehicleCategoryId]
  def get: F[List[VehicleCategory]]
  def update(input: VehicleCategory): F[Unit]
}

object VehicleCategoriesAlgebra {
  def make[F[_]: MonadThrow: GenUUID](
      vehicleCategoriesRepository: VehicleCategoriesRepository[F]
    ): VehicleCategoriesAlgebra[F] =
    new VehicleCategoriesAlgebra[F] {
      override def create(name: NonEmptyString): F[VehicleCategoryId] =
        for {
          id <- ID.make[F, VehicleCategoryId]
          dtoBranch = dto.VehicleCategory(
            id = id,
            name = name,
          )
          _ <- vehicleCategoriesRepository.create(dtoBranch)
        } yield id

      override def get: F[List[VehicleCategory]] =
        vehicleCategoriesRepository
          .get
          .map(
            _.map(vc =>
              VehicleCategory(
                id = vc.id,
                name = vc.name,
              )
            )
          )

      override def update(input: VehicleCategory): F[Unit] =
        vehicleCategoriesRepository.update(input.id)(_.copy(name = input.name))
    }
}
