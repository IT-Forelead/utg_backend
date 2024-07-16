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
}

object VehiclesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      vehiclesRepository: VehiclesRepository[F],
      branchesRepository: BranchesRepository[F],
      vehicleCategoriesRepository: VehicleCategoriesRepository[F],
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
            gpsTracker = vehicleInput.gpsTracker,
            fuelLevelSensor = vehicleInput.fuelLevelSensor,
            fuelTankVolume = vehicleInput.fuelTankVolume,
          )
          _ <- vehiclesRepository.create(dtoVehicle)
        } yield id

      override def get(filters: VehicleFilters): F[ResponseData[Vehicle]] =
        for {
          dtoVehicles <- vehiclesRepository.get(filters)
          branches <- branchesRepository.findByIds(dtoVehicles.data.map(_.branchId))
          vehicleCategories <- vehicleCategoriesRepository.findByIds(
            dtoVehicles.data.map(_.vehicleCategoryId)
          )
          vehicles = dtoVehicles.data.map { v =>
            Vehicle(
              id = v.id,
              createdAt = v.createdAt,
              branch = branches.get(v.branchId),
              vehicleCategory = vehicleCategories.get(v.vehicleCategoryId),
              brand = v.brand,
              registeredNumber = v.registeredNumber,
              inventoryNumber = v.inventoryNumber,
              yearOfRelease = v.yearOfRelease,
              bodyNumber = v.bodyNumber,
              chassisNumber = v.chassisNumber,
              engineNumber = v.engineNumber,
              conditionType = v.conditionType,
              fuelType = v.fuelType,
              description = v.description,
              gpsTracker = v.gpsTracker,
              fuelLevelSensor = v.fuelLevelSensor,
              fuelTankVolume = v.fuelTankVolume,
            )
          }
        } yield dtoVehicles.copy(data = vehicles)
    }
}
