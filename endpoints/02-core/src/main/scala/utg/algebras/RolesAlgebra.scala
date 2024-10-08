package utg.algebras

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import eu.timepit.refined.types.string.NonEmptyString

import utg.domain.Role
import utg.domain.RoleId
import utg.effects.GenUUID
import utg.repos.RolesRepository
import utg.utils.ID

trait RolesAlgebra[F[_]] {
  def getAll: F[List[Role]]
  def createRole(name: NonEmptyString): F[Unit]
}

object RolesAlgebra {
  def make[F[_]: MonadThrow: GenUUID](
      rolesRepository: RolesRepository[F]
    ): RolesAlgebra[F] =
    new RolesAlgebra[F] {
      override def getAll: F[List[Role]] = rolesRepository.getAll
      override def createRole(name: NonEmptyString): F[Unit] =
        ID.make[F, RoleId].flatMap { id =>
          rolesRepository.createRole(id, name)
        }
    }
}
