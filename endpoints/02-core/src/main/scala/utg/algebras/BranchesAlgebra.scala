package utg.algebras

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import shapeless.syntax.std.tuple.productTupleOps

import utg.domain.Branch
import utg.domain.BranchId
import utg.domain.args.branches.BranchInput
import utg.effects.GenUUID
import utg.repos.BranchesRepository
import utg.repos.RegionsRepository
import utg.repos.sql.dto
import utg.utils.ID

trait BranchesAlgebra[F[_]] {
  def create(input: BranchInput): F[BranchId]
  def getBranches: F[List[Branch]]
}

object BranchesAlgebra {
  def make[F[_]: MonadThrow: GenUUID](
      branchesRepository: BranchesRepository[F],
      regionsRepository: RegionsRepository[F],
    ): BranchesAlgebra[F] =
    new BranchesAlgebra[F] {
      override def create(input: BranchInput): F[BranchId] =
        for {
          id <- ID.make[F, BranchId]
          dtoBranch = dto.Branch(
            id = id,
            name = input.name,
            code = input.code,
            regionId = input.regionId,
          )
          _ <- branchesRepository.create(dtoBranch)
        } yield id

      override def getBranches: F[List[Branch]] =
        for {
          branches <- branchesRepository.getBranches
          regions <- regionsRepository.findByIds(branches.map(_.regionId))
          roles = branches.map { branch =>
            Branch(
              id = branch.id,
              name = branch.name,
              code = branch.code,
              region = regions.get(branch.regionId),
            )
          }
        } yield roles
    }
}
