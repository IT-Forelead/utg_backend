package utg.algebras

import cats.MonadThrow
import cats.implicits._
import cats.effect.std.Random
import org.typelevel.log4cats.Logger
import utg.domain.{CompleteTask, CompleteTaskId, ResponseData}
import utg.domain.args.completeTasks.{CompleteTaskFilters, CompleteTaskInput, UpdateCompleteTaskInput}
import utg.effects.{Calendar, GenUUID}
import utg.repos.CompleteTasksRepository
import utg.repos.sql.dto
import utg.utils.ID

trait CompleteTasksAlgebra[F[_]] {
  def get(filters: CompleteTaskFilters): F[ResponseData[CompleteTask]]
  def findById(id: CompleteTaskId): F[Option[CompleteTask]]
  def create(completeTaskInput: CompleteTaskInput): F[CompleteTaskId]
  def update(
      id: CompleteTaskId,
      completeTaskInput: UpdateCompleteTaskInput,
    ): F[Unit]
  def delete(id: CompleteTaskId): F[Unit]
  
}

object CompleteTasksAlgebra {
  def make[F[_]: Calendar: GenUUID: Random](
                                             completeTasksRepository: CompleteTasksRepository[F],
                                             assets: AssetsAlgebra[F],
                                           )(implicit
                                             F: MonadThrow[F],
                                             logger: Logger[F],
                                           ): CompleteTasksAlgebra[F] =
    new CompleteTasksAlgebra[F] {
      override def get(filters: CompleteTaskFilters): F[ResponseData[CompleteTask]] =
        completeTasksRepository.get(filters)

      override def findById(id: CompleteTaskId): F[Option[CompleteTask]] =
        completeTasksRepository.findById(id)

      override def create(completeTaskInput: CompleteTaskInput): F[CompleteTaskId] =
        for {
          id <- ID.make[F, CompleteTaskId]
          completeTask = dto.CompleteTask(
            id = id,
            tripNumber = completeTaskInput.tripNumber,
            invoiceNumber = completeTaskInput.invoiceNumber,
            arrivalTime = completeTaskInput.arrivalTime,
            consignorSignId = completeTaskInput.consignorSignId,
            documentId = completeTaskInput.documentId
          )
          _ <- completeTasksRepository.create(completeTask)
        } yield id

      override def update(
                           id: CompleteTaskId,
                           completeTaskInput: UpdateCompleteTaskInput,
                         ): F[Unit] =
        for {
          _ <- completeTasksRepository.update(id)(
            _.copy(
              tripNumber = completeTaskInput.tripNumber,
              invoiceNumber = completeTaskInput.invoiceNumber,
              arrivalTime = completeTaskInput.arrivalTime,
              consignorSignId = completeTaskInput.consignorSignId,
              documentId = completeTaskInput.documentId
            )
          )
        } yield {}

      override def delete(id: CompleteTaskId): F[Unit] =
        completeTasksRepository.delete(id)

    }
}


