package utg.algebras

import cats.MonadThrow
import cats.effect.std.Random
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import org.typelevel.log4cats.Logger

import utg.domain.LineDelay
import utg.domain.LineDelayId
import utg.domain.ResponseData
import utg.domain.args.lineDelays.LineDelayFilters
import utg.domain.args.lineDelays.LineDelayInput
import utg.domain.args.lineDelays.UpdateLineDelayInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.LineDelaysRepository
import utg.repos.sql.dto
import utg.utils.ID

trait LineDelaysAlgebra[F[_]] {
  def get(filters: LineDelayFilters): F[ResponseData[LineDelay]]
  def findById(id: LineDelayId): F[Option[LineDelay]]
  def create(LineDelayInput: LineDelayInput): F[LineDelayId]
  def update(
      id: LineDelayId,
      LineDelayInput: UpdateLineDelayInput,
    ): F[Unit]
  def delete(id: LineDelayId): F[Unit]
}
object LineDelaysAlgebra {
  def make[F[_]: Calendar: GenUUID: Random](
      lineDelaysRepository: LineDelaysRepository[F]
    )(implicit
      F: MonadThrow[F],
      logger: Logger[F],
    ): LineDelaysAlgebra[F] =
    new LineDelaysAlgebra[F] {
      override def get(filters: LineDelayFilters): F[ResponseData[LineDelay]] =
        lineDelaysRepository.get(filters)

      override def findById(id: LineDelayId): F[Option[LineDelay]] =
        lineDelaysRepository.findById(id)

      override def create(lineDelayInput: LineDelayInput): F[LineDelayId] =
        for {
          id <- ID.make[F, LineDelayId]
          lineDelay = dto.LineDelay(
            id = id,
            name = lineDelayInput.name,
            startTime = lineDelayInput.startTime,
            endTime = lineDelayInput.endTime,
            signId = lineDelayInput.signId,
          )
          _ <- lineDelaysRepository.create(lineDelay)
        } yield id

      override def update(
          id: LineDelayId,
          lineDelayInput: UpdateLineDelayInput,
        ): F[Unit] =
        for {
          _ <- lineDelaysRepository.update(id)(
            _.copy(
              name = lineDelayInput.name,
              startTime = lineDelayInput.startTime,
              endTime = lineDelayInput.endTime,
              signId = lineDelayInput.signId,
            )
          )
        } yield {}

      override def delete(id: LineDelayId): F[Unit] =
        lineDelaysRepository.delete(id)
    }
}
