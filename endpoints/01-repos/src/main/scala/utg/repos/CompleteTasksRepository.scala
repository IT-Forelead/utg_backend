package utg.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.toFunctorOps
import skunk.Session
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.domain.CompleteTask
import utg.domain.CompleteTaskId
import utg.domain.ResponseData
import utg.domain.args.completeTasks.CompleteTaskFilters
import utg.exception.AError
import utg.repos.sql.CompleteTasksSql
import utg.repos.sql.dto

trait CompleteTasksRepository[F[_]] {
  def findById(id: CompleteTaskId): F[Option[CompleteTask]]
  def create(completeTask: dto.CompleteTask): F[Unit]
  def update(id: CompleteTaskId)(update: dto.CompleteTask => dto.CompleteTask): F[Unit]
  def delete(id: CompleteTaskId): F[Unit]
  def get(filters: CompleteTaskFilters): F[ResponseData[CompleteTask]]
}

object CompleteTasksRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): CompleteTasksRepository[F] = new CompleteTasksRepository[F] {
    private def makeCompleteTask(completeTaskDto: dto.CompleteTask): CompleteTask =
      completeTaskDto.toDomain

    private def makeCompleteTasks(dtos: List[dto.CompleteTask]): List[CompleteTask] =
      dtos.map { completeTaskDto =>
        completeTaskDto.toDomain
      }

    override def findById(id: CompleteTaskId): F[Option[CompleteTask]] =
      OptionT(CompleteTasksSql.findById.queryOption(id)).map(makeCompleteTask).value

    override def create(completeTaskAndHash: dto.CompleteTask): F[Unit] =
      CompleteTasksSql.insert.execute(completeTaskAndHash)

    override def get(filters: CompleteTaskFilters): F[ResponseData[CompleteTask]] = {
      val af = CompleteTasksSql
        .select(filters)
        .paginateOpt(filters.limit.map(_.value), filters.page.map(_.value))
      af.fragment.query(CompleteTasksSql.codec *: int8).queryList(af.argument).map {
        completeTasksDto =>
          ResponseData(
            makeCompleteTasks(completeTasksDto.map(_.head)),
            completeTasksDto.headOption.fold(0L)(_.tail.head),
          )
      }
    }

    override def update(id: CompleteTaskId)(update: dto.CompleteTask => dto.CompleteTask): F[Unit] =
      OptionT(CompleteTasksSql.findById.queryOption(id)).cataF(
        AError.Internal(s"Complete Task not found by id [$id]").raiseError[F, Unit],
        completeTask => CompleteTasksSql.update.execute(update(completeTask)),
      )

    override def delete(id: CompleteTaskId): F[Unit] =
      CompleteTasksSql.delete.execute(id)
  }
}
