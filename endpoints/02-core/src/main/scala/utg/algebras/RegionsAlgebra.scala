package utg.algebras

import cats.MonadThrow

import utg.domain.Region
import utg.repos.RegionsRepository

trait RegionsAlgebra[F[_]] {
  def getRegions: F[List[Region]]
}

object RegionsAlgebra {
  def make[F[_]: MonadThrow](
      regionsRepository: RegionsRepository[F]
    ): RegionsAlgebra[F] =
    new RegionsAlgebra[F] {
      override def getRegions: F[List[Region]] =
        regionsRepository.getRegions
    }
}
