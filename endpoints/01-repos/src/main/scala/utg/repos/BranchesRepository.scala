package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.repos.sql.BranchesSql
import utg.repos.sql.dto

trait BranchesRepository[F[_]] {
  def create(branch: dto.Branch): F[Unit]
  def getBranches: F[List[dto.Branch]]
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
  }
}
