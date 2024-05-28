package utg.algebras

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps

import utg.domain.ResponseData
import utg.domain.Vehicle
import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters
import utg.domain.args.vehicles.VehicleInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.VehiclesRepository
import utg.utils.ID

trait VehiclesAlgebra[F[_]] {
  def create(vehicleInput: VehicleInput): F[VehicleId]
  def get(filters: VehicleFilters): F[ResponseData[Vehicle]]
}

object VehiclesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      vehiclesRepository: VehiclesRepository[F]
    ): VehiclesAlgebra[F] =
    new VehiclesAlgebra[F] {
      override def create(vehicleInput: VehicleInput): F[VehicleId] =
        for {
          id <- ID.make[F, VehicleId]
          now <- Calendar[F].currentZonedDateTime
          vehicle = Vehicle(
            id = id,
            createdAt = now,
            name = vehicleInput.name,
            registeredNumber = vehicleInput.registeredNumber,
            vehicleType = vehicleInput.vehicleType,
            fuelTankVolume = vehicleInput.fuelTankVolume,
          )
          _ <- vehiclesRepository.create(vehicle)
        } yield id

      override def get(filters: VehicleFilters): F[ResponseData[Vehicle]] =
        vehiclesRepository.get(filters)
    }
}
