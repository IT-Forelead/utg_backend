package utg.repos

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.Branch
import utg.domain.BranchId
import utg.exception.AError
import utg.repos.sql.BranchesSql
import utg.repos.sql.dto

trait BranchesRepository[F[_]] {
  def create(branch: dto.Branch): F[Unit]
  def getBranches: F[List[dto.Branch]]
  def update(id: BranchId)(update: dto.Branch => dto.Branch): F[Unit]
  def findByIds(ids: List[BranchId]): F[Map[BranchId, Branch]]
}

object BranchesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): BranchesRepository[F] = new BranchesRepository[F] {
    override def create(branch: dto.Branch): F[Unit] =
      BranchesSql.insert.execute(branch)

    override def getBranches: F[List[dto.Branch]] =
      BranchesSql.selectBranches.queryList(Void)

    override def update(id: BranchId)(update: dto.Branch => dto.Branch): F[Unit] =
      OptionT(BranchesSql.findById.queryOption(id)).cataF(
        AError.Internal(s"Branch not found by id [$id]").raiseError[F, Unit],
        branch => BranchesSql.update.execute(update(branch)),
      )

    override def findByIds(
        ids: List[BranchId]
      ): F[Map[BranchId, Branch]] =
      NonEmptyList.fromList(ids).fold(Map.empty[BranchId, Branch].pure[F]) { rIds =>
        val branchIds = rIds.toList
        BranchesSql.findByIds(branchIds).queryList(branchIds).map {
          _.map { dto =>
            dto.id -> Branch(dto.id, dto.name, dto.code, None)
          }.toMap
        }
      }
  }
}
