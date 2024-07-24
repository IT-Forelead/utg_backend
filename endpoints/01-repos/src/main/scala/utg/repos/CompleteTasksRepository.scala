package utg.repos

import cats.data.{NonEmptyList, OptionT}
import cats.effect.{Async, Resource}
import cats.implicits.{catsSyntaxApplicativeErrorId, toFlatMapOps, toFunctorOps}
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Session
import skunk.codec.all.int8
import utg.domain.args.completeTasks.CompleteTaskFilters
import utg.domain.{CompleteTask, CompleteTaskId, RegionId, ResponseData, Role, auth}
import utg.exception.AError
import utg.repos.sql.{BranchesSql, CompleteTasksSql, RegionsSql, RolesSql, dto}
import uz.scala.skunk.syntax.all.{skunkSyntaxCommandOps, skunkSyntaxFragmentOps, skunkSyntaxQueryOps}
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

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
    private def makeCompleteTask(completeTaskDto: dto.CompleteTask): F[Option[CompleteTask]] =


    private def makeCompleteTasks(dtos: List[dto.CompleteTask]): F[List[CompleteTask]] = {

    }

    override def findById(id: CompleteTaskId): F[Option[CompleteTask]] =
      OptionT(CompleteTasksSql.findById.queryOption(id)).flatMapF(makeCompleteTask).value

    override def create(completeTaskAndHash: dto.CompleteTask): F[Unit] =
      CompleteTasksSql.insert.execute(completeTaskAndHash)

    override def get(filters: CompleteTaskFilters): F[ResponseData[CompleteTask]] = {
      val af = CompleteTasksSql
        .select(filters)
        .paginateOpt(filters.limit.map(_.value), filters.page.map(_.value))
      af.fragment.query(CompleteTasksSql.codec *: int8).queryList(af.argument).flatMap { completeTasksDto =>
        makeCompleteTasks(completeTasksDto.map(_.head)).map { completeTasks =>
          ResponseData(completeTasks, completeTasksDto.headOption.fold(0L)(_.tail.head))
        }
      }
    }

    override def update(id: CompleteTaskId)(update: dto.CompleteTask => dto.CompleteTask): F[Unit] =
      OptionT(CompleteTasksSql.findById.queryOption(id)).cataF(
        AError.Internal(s"CompleteTask not found by id [$id]").raiseError[F, Unit],
        completeTask => CompleteTasksSql.update.execute(update(completeTask)),
      )

    override def delete(id: CompleteTaskId): F[Unit] =
      CompleteTasksSql.delete.execute(id)

  }
}



