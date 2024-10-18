package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.implicits._
import cats.implicits.toFlatMapOps

import utg.domain.AssetId
import utg.domain.FuelTypeAndQuantity
import utg.domain.ResponseData
import utg.domain.Vehicle
import utg.domain.Vehicle.VehicleInfo
import utg.domain.VehicleId
import utg.domain.args.vehicles._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.VehicleFuelItemsRepository
import utg.repos.VehicleLicensePhotosRepository
import utg.repos.VehiclePhotosRepository
import utg.repos.VehiclesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait VehiclesAlgebra[F[_]] {
  def create(input: VehicleInput): F[VehicleId]
  def get(filters: VehicleFilters): F[ResponseData[VehicleInfo]]
  def getAsStream(filters: VehicleFilters): F[fs2.Stream[F, Vehicle]]
  def update(input: UpdateVehicleInput): F[Unit]
}

object VehiclesAlgebra {
  def make[F[_]: Calendar: GenUUID](
      vehiclesRepository: VehiclesRepository[F],
      vehicleFuelItemsRepository: VehicleFuelItemsRepository[F],
      vehiclePhotosRepository: VehiclePhotosRepository[F],
      vehicleLicensePhotosRepository: VehicleLicensePhotosRepository[F],
      assetsAlgebra: AssetsAlgebra[F],
    )(implicit
      F: MonadThrow[F]
    ): VehiclesAlgebra[F] =
    new VehiclesAlgebra[F] {
      override def create(input: VehicleInput): F[VehicleId] =
        for {
          id <- ID.make[F, VehicleId]
          now <- Calendar[F].currentZonedDateTime
          dtoVehicle = dto.Vehicle(
            id = id,
            createdAt = now,
            vehicleType = input.vehicleType,
            registeredNumber = input.registeredNumber,
            brand = input.brand,
            color = input.color,
            owner = input.owner,
            address = input.address,
            dateOfIssue = input.dateOfIssue,
            issuingAuthority = input.issuingAuthority,
            pin = input.pin,
            yearOfRelease = input.yearOfRelease,
            vehicleCategoryId = input.vehicleCategoryId,
            bodyNumber = input.bodyNumber,
            chassisNumber = input.chassisNumber,
            maxMass = input.maxMass,
            unloadMass = input.unloadMass,
            engineNumber = input.engineNumber,
            engineCapacity = input.engineCapacity,
            numberOfSeats = input.numberOfSeats,
            numberOfStandingPlaces = input.numberOfStandingPlaces,
            specialMarks = input.specialMarks,
            licenseNumber = input.licenseNumber,
            branchId = input.branchId,
            inventoryNumber = input.inventoryNumber,
            conditionType = input.conditionType,
            gpsTracking = input.gpsTracking,
            fuelLevelSensor = input.fuelLevelSensor,
            description = input.description,
          )
          _ <- vehiclesRepository.create(dtoVehicle)
          _ <- input.fuels.traverse { fuels =>
            vehicleFuelItemsRepository.create(id, fuels)
          }
          _ <- input.vehiclePhotoIds.traverse { photoIds =>
            vehiclePhotosRepository.create(id, photoIds)
          }
          _ <- input.licensePhotoIds.traverse { photoIds =>
            vehicleLicensePhotosRepository.create(id, photoIds)
          }
        } yield id

      override def get(filters: VehicleFilters): F[ResponseData[VehicleInfo]] =
        for {
          vehicles <- vehiclesRepository.get(filters)
          assetIds = vehicles
            .data
            .flatMap(v => v.vehiclePhotoIds ++ v.vehicleLicensePhotoIds)
            .distinct
          vehicleAssets <- assetsAlgebra.getByIds(assetIds)
          data = vehicles
            .data
            .map(v =>
              v.toDomain(
                vehicleAssets.filter(ai => v.vehiclePhotoIds.contains(ai.id)),
                vehicleAssets.filter(ai => v.vehicleLicensePhotoIds.contains(ai.id)),
              )
            )
        } yield ResponseData(data, vehicles.total)

      override def getAsStream(filters: VehicleFilters): F[fs2.Stream[F, Vehicle]] =
        vehiclesRepository
          .getAsStream(filters)
          .evalMap { vehicle =>
            vehiclesRepository.makeVehicle(vehicle)
          }
          .pure[F]

      override def update(input: UpdateVehicleInput): F[Unit] =
        for {
          _ <- vehiclesRepository.update(input.id) { dtoVehicle =>
            dtoVehicle.copy(
              vehicleType = input.vehicleType,
              registeredNumber = input.registeredNumber,
              brand = input.brand,
              color = input.color,
              owner = input.owner,
              address = input.address,
              dateOfIssue = input.dateOfIssue,
              issuingAuthority = input.issuingAuthority,
              pin = input.pin,
              yearOfRelease = input.yearOfRelease,
              vehicleCategoryId = input.vehicleCategoryId,
              bodyNumber = input.bodyNumber,
              chassisNumber = input.chassisNumber,
              maxMass = input.maxMass,
              unloadMass = input.unloadMass,
              engineNumber = input.engineNumber,
              engineCapacity = input.engineCapacity,
              numberOfSeats = input.numberOfSeats,
              numberOfStandingPlaces = input.numberOfStandingPlaces,
              specialMarks = input.specialMarks,
              licenseNumber = input.licenseNumber,
              branchId = input.branchId,
              conditionType = input.conditionType,
              gpsTracking = input.gpsTracking,
              fuelLevelSensor = input.fuelLevelSensor,
              description = input.description,
            )
          }
          _ <- input.fuels.traverse { fuels =>
            updateVehicleFuelItems(input.id, fuels)
          }
          _ <- input.vehiclePhotoIds.traverse { assetIds =>
            updateVehiclePhotos(input.id, assetIds)
          }
          _ <- input.licensePhotoIds.traverse { assetIds =>
            updateVehicleLicensePhotos(input.id, assetIds)
          }
        } yield ()

      private def updateVehicleFuelItems(
          vehicleId: VehicleId,
          fuels: NonEmptyList[FuelTypeAndQuantity],
        ): F[Unit] =
        for {
          _ <- vehicleFuelItemsRepository.deleteByVehicleId(vehicleId)
          _ <- fuels.traverse_ { item =>
            vehicleFuelItemsRepository.create(vehicleId, NonEmptyList.one(item))
          }
        } yield ()

      private def updateVehiclePhotos(
          vehicleId: VehicleId,
          assetIds: NonEmptyList[AssetId],
        ): F[Unit] =
        for {
          _ <- vehiclePhotosRepository.deleteByVehicleId(vehicleId)
          _ <- assetIds.traverse_ { assetId =>
            vehiclePhotosRepository.create(vehicleId, NonEmptyList.one(assetId))
          }
        } yield ()

      private def updateVehicleLicensePhotos(
          vehicleId: VehicleId,
          assetIds: NonEmptyList[AssetId],
        ): F[Unit] =
        for {
          _ <- vehicleLicensePhotosRepository.deleteByVehicleId(vehicleId)
          _ <- assetIds.traverse_ { assetId =>
            vehicleLicensePhotosRepository.create(vehicleId, NonEmptyList.one(assetId))
          }
        } yield ()
    }
}
