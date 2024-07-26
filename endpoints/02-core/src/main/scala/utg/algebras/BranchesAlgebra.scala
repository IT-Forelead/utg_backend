package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.domain.Branch
import utg.domain.BranchId
import utg.domain.Region
import utg.domain.RegionId
import utg.domain.args.branches._
import utg.domain.generateShortHash
import utg.effects.GenUUID
import utg.repos.BranchesRepository
import utg.repos.RegionsRepository
import utg.repos.sql.dto
import utg.utils.ID

trait BranchesAlgebra[F[_]] {
  def create(input: BranchInput): F[BranchId]
  def getBranches: F[List[Branch]]
  def update(input: UpdateBranchInput): F[Unit]
  def getAsStream(filter: BranchFilters): F[fs2.Stream[F, Branch]]
}

object BranchesAlgebra {
  def make[F[_]: GenUUID](
      branchesRepository: BranchesRepository[F],
      regionsRepository: RegionsRepository[F],
    )(implicit
      F: MonadThrow[F]
    ): BranchesAlgebra[F] =
    new BranchesAlgebra[F] {
      override def create(input: BranchInput): F[BranchId] =
        for {
          id <- ID.make[F, BranchId]
          dtoBranch = dto.Branch(
            id = id,
            name = input.name,
            code = generateShortHash(input.name.value),
            regionId = input.regionId,
          )
          _ <- branchesRepository.create(dtoBranch)
        } yield id

      override def getBranches: F[List[Branch]] =
        for {
          branches <- branchesRepository.getBranches
          regions <- NonEmptyList
            .fromList(branches.map(_.regionId))
            .fold(Map.empty[RegionId, Region].pure[F]) { regionIds =>
              regionsRepository.findByIds(regionIds.toList)
            }
          roles = branches.map { branch =>
            Branch(
              id = branch.id,
              name = branch.name,
              code = branch.code,
              region = regions.get(branch.regionId),
            )
          }
        } yield roles

      override def update(input: UpdateBranchInput): F[Unit] =
        branchesRepository.update(input.id)(
          _.copy(
            name = input.name,
            regionId = input.regionId,
          )
        )

      override def getAsStream(filters: BranchFilters): F[fs2.Stream[F, Branch]] =
        F.pure {
          branchesRepository.getAsStream(filters).evalMap { branch =>
            branchesRepository.makeBranch(branch)
          }
        }
    }
}
