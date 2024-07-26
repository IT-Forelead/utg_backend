package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.std.Random
import cats.implicits._
import org.typelevel.log4cats.Logger

import utg.domain.CompleteTask
import utg.domain.CompleteTaskId
import utg.domain.ResponseData
import utg.domain.args.completeTasks.CompleteTaskFilters
import utg.domain.args.completeTasks.CompleteTaskInput
import utg.domain.args.completeTasks.UpdateCompleteTaskInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.CompleteTasksRepository
import utg.repos.TripsRepository
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
      tripsRepository: TripsRepository[F],
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
        OptionT(tripsRepository.findById(completeTaskInput.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$completeTaskInput.tripId]")
            .raiseError[F, CompleteTaskId],
          trip =>
            for {
              id <- ID.make[F, CompleteTaskId]
              now <- Calendar[F].currentZonedDateTime
              completeTask = dto.CompleteTask(
                id = id,
                createdAt = now,
                tripId = trip.id,
                tripNumber = completeTaskInput.tripNumber,
                invoiceNumber = completeTaskInput.invoiceNumber,
                arrivalTime = completeTaskInput.arrivalTime,
                consignorSignId = completeTaskInput.consignorSignId,
                documentId = completeTaskInput.documentId,
              )
              _ <- completeTasksRepository.create(completeTask)
            } yield id,
        )

      override def update(
          id: CompleteTaskId,
          completeTaskInput: UpdateCompleteTaskInput,
        ): F[Unit] =
        for {
          _ <- completeTasksRepository.update(id)(
            _.copy(
              tripId = completeTaskInput.tripId,
              tripNumber = completeTaskInput.tripNumber,
              invoiceNumber = completeTaskInput.invoiceNumber,
              arrivalTime = completeTaskInput.arrivalTime,
              consignorSignId = completeTaskInput.consignorSignId,
              documentId = completeTaskInput.documentId,
            )
          )
        } yield {}

      override def delete(id: CompleteTaskId): F[Unit] =
        completeTasksRepository.delete(id)
    }
}
