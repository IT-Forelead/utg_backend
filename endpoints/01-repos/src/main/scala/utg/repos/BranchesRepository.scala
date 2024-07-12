package utg.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.BranchId
import utg.exception.AError
import utg.repos.sql.BranchesSql
import utg.repos.sql.dto

trait BranchesRepository[F[_]] {
  def create(branch: dto.Branch): F[Unit]
  def getBranches: F[List[dto.Branch]]
  def update(id: BranchId)(update: dto.Branch => dto.Branch): F[Unit]
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
  }
}
