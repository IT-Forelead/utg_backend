package utg.algebras

import cats.MonadThrow
import cats.implicits._
import utg.domain.ResponseData
import utg.domain.Vehicle
import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters
import utg.domain.args.vehicles.VehicleInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.BranchesRepository
import utg.repos.VehicleCategoriesRepository
import utg.repos.VehiclesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait VehiclesAlgebra[F[_]] {
  def create(vehicleInput: VehicleInput): F[VehicleId]
  def get(filters: VehicleFilters): F[ResponseData[Vehicle]]
  def getAsStream(filters: VehicleFilters): F[fs2.Stream[F, Vehicle]]
}

object VehiclesAlgebra {
  def make[F[_]: Calendar: GenUUID](
      vehiclesRepository: VehiclesRepository[F],
      branchesRepository: BranchesRepository[F],
      vehicleCategoriesRepository: VehicleCategoriesRepository[F],
    )(implicit
      F: MonadThrow[F],
     ): VehiclesAlgebra[F] =
    new VehiclesAlgebra[F] {
      override def create(vehicleInput: VehicleInput): F[VehicleId] =
        for {
          id <- ID.make[F, VehicleId]
          now <- Calendar[F].currentZonedDateTime
          dtoVehicle = dto.Vehicle(
            id = id,
            createdAt = now,
            branchId = vehicleInput.branchId,
            vehicleCategoryId = vehicleInput.vehicleCategoryId,
            vehicleType = vehicleInput.vehicleType,
            brand = vehicleInput.brand,
            registeredNumber = vehicleInput.registeredNumber,
            inventoryNumber = vehicleInput.inventoryNumber,
            yearOfRelease = vehicleInput.yearOfRelease,
            bodyNumber = vehicleInput.bodyNumber,
            chassisNumber = vehicleInput.chassisNumber,
            engineNumber = vehicleInput.engineNumber,
            conditionType = vehicleInput.conditionType,
            fuelType = vehicleInput.fuelType,
            description = vehicleInput.description,
            gpsTracking = vehicleInput.gpsTracking,
            fuelLevelSensor = vehicleInput.fuelLevelSensor,
            fuelTankVolume = vehicleInput.fuelTankVolume,
          )
          _ <- vehiclesRepository.create(dtoVehicle)
        } yield id

      override def get(filters: VehicleFilters): F[ResponseData[Vehicle]] =
        vehiclesRepository.get(filters)

      override def getAsStream(filters: VehicleFilters): F[fs2.Stream[F, Vehicle]] =
        F.pure {
          vehiclesRepository.getAsStream(filters).evalMap { vehicle =>
            vehiclesRepository.makeVehicle(vehicle)
          }
        }
    }
}
