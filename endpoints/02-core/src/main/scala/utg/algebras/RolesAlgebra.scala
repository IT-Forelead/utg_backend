package utg.algebras

import cats.MonadThrow

import utg.domain.Role
import utg.repos.RolesRepository

trait RolesAlgebra[F[_]] {
  def getAll: F[List[Role]]
}

object RolesAlgebra {
  def make[F[_]: MonadThrow](
      rolesRepository: RolesRepository[F]
    ): RolesAlgebra[F] =
    new RolesAlgebra[F] {
      override def getAll: F[List[Role]] = rolesRepository.getAll
    }
}
