package utg.algebras

import cats.MonadThrow
import cats.implicits._

import utg.domain.ResponseData
import utg.domain.VehicleHistory
import utg.domain.VehicleHistoryId
import utg.domain.args.vehicleHistories.VehicleHistoryFilters
import utg.domain.args.vehicleHistories.VehicleHistoryInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.VehicleHistoriesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait VehicleHistoriesAlgebra[F[_]] {
  def create(vehicleHistoryInput: VehicleHistoryInput): F[VehicleHistoryId]
  def get(filters: VehicleHistoryFilters): F[ResponseData[VehicleHistory]]
  def getAsStream(filters: VehicleHistoryFilters): F[fs2.Stream[F, VehicleHistory]]
}

object VehicleHistoriesAlgebra {
  def make[F[_]: Calendar: GenUUID](
      vehicleHistoriesRepository: VehicleHistoriesRepository[F]
    )(implicit
      F: MonadThrow[F]
    ): VehicleHistoriesAlgebra[F] =
    new VehicleHistoriesAlgebra[F] {
      override def create(vehicleHistoryInput: VehicleHistoryInput): F[VehicleHistoryId] =
        for {
          id <- ID.make[F, VehicleHistoryId]
          now <- Calendar[F].currentZonedDateTime
          dtoVehicleHistory = dto.VehicleHistory(
            id = id,
            createdAt = now,
            vehicleId = vehicleHistoryInput.vehicleId,
            branchId = vehicleHistoryInput.branchId,
            registeredNumber = vehicleHistoryInput.registeredNumber,
          )
          _ <- vehicleHistoriesRepository.create(dtoVehicleHistory)
        } yield id

      override def get(filters: VehicleHistoryFilters): F[ResponseData[VehicleHistory]] =
        vehicleHistoriesRepository.get(filters)

      override def getAsStream(filters: VehicleHistoryFilters): F[fs2.Stream[F, VehicleHistory]] =
        F.pure {
          vehicleHistoriesRepository.getAsStream(filters).evalMap { vehicleHistory =>
            vehicleHistoriesRepository.makeVehicleHistory(vehicleHistory)
          }
        }
    }
}
