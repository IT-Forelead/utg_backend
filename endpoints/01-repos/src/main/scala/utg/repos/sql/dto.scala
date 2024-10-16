package utg.repos.sql

import java.io.StringWriter
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import cats.data.NonEmptyList
import cats.effect.Concurrent
import cats.effect.Sync
import cats.implicits.catsSyntaxOptionId
import com.github.tototoshi.csv.CSVWriter
import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import fs2.text.utf8
import io.scalaland.chimney.dsl._

import utg._
import utg.domain
import utg.domain._
import utg.domain.enums._

object dto {
  case class User(
      id: UserId,
      createdAt: ZonedDateTime,
      firstname: NonEmptyString,
      lastname: NonEmptyString,
      middleName: Option[NonEmptyString],
      birthday: Option[LocalDate],
      personalNumber: NonNegInt,
      phone: Phone,
      roleId: RoleId,
      assetId: Option[AssetId],
      branchCode: Option[NonEmptyString],
      drivingLicenseNumber: Option[NonEmptyString],
      drivingLicenseCategories: Option[List[DrivingLicenseCategory]],
      drivingLicenseGiven: Option[LocalDate],
      drivingLicenseExpire: Option[LocalDate],
      machineOperatorLicenseNumber: Option[NonEmptyString],
      machineOperatorLicenseCategories: Option[List[MachineOperatorLicenseCategory]],
      machineOperatorLicenseGiven: Option[LocalDate],
      machineOperatorLicenseExpire: Option[LocalDate],
    ) {
    def toDomain(
        role: domain.Role,
        branch: Option[utg.domain.Branch],
        drivingLicenseCategories: Option[NonEmptyList[DrivingLicenseCategory]],
        machineOperatorLicenseCategories: Option[NonEmptyList[MachineOperatorLicenseCategory]],
      ): AuthedUser.User =
      this
        .into[AuthedUser.User]
        .withFieldConst(_.role, role)
        .withFieldConst(_.phone, phone)
        .withFieldConst(_.branch, branch)
        .withFieldConst(_.drivingLicenseCategories, drivingLicenseCategories)
        .withFieldConst(_.machineOperatorLicenseCategories, machineOperatorLicenseCategories)
        .transform

    private def ldtToString(date: ZonedDateTime, format: String = "yyyy MM dd HH:mm"): String =
      date.format(DateTimeFormatter.ofPattern(format))

    private def toCSVField: List[String] =
      List[String](
        ldtToString(createdAt),
        firstname.value,
        lastname.value,
        phone.value,
      )
  }

  object User {
    def fromDomain(user: AuthedUser.User): User =
      user
        .into[User]
        .withFieldConst(_.roleId, user.role.id)
        .withFieldConst(_.branchCode, user.branch.map(_.code))
        .withFieldConst(_.drivingLicenseCategories, user.drivingLicenseCategories.map(_.toList))
        .withFieldConst(
          _.machineOperatorLicenseCategories,
          user.machineOperatorLicenseCategories.map(_.toList),
        )
        .transform

    private val CsvHeaders: List[String] =
      List(
        "Created Date",
        "First Name",
        "Last Name",
        "Phone",
      )

    def makeCsv[F[_]: Concurrent: Sync]: fs2.Pipe[F, dto.User, Byte] =
      report =>
        fs2
          .Stream
          .fromIterator[F](List(CsvHeaders).map(writeAsCsv).iterator, 128)
          .merge(report.map(_.toCSVField).map(writeAsCsv))
          .through(utf8.encode)

    private def writeAsCsv(rows: List[String]): String = {
      val writer = new StringWriter()
      val csvWriter = CSVWriter.open(writer)
      csvWriter.writeRow(rows)
      writer.toString
    }
  }

  case class Role(id: RoleId, name: NonEmptyString)

  case class Region(
      id: RegionId,
      name: NonEmptyString,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.Region =
      this.transformInto[domain.Region]
  }

  case class Branch(
      id: BranchId,
      name: NonEmptyString,
      code: NonEmptyString,
      regionId: RegionId,
      deleted: Boolean = false,
    ) {
    def toDomain(region: Option[domain.Region]): domain.Branch =
      this
        .into[domain.Branch]
        .withFieldConst(_.region, region)
        .transform
  }

  case class SmsMessage(
      id: SmsMessageId,
      createdAt: ZonedDateTime,
      phone: Phone,
      text: NonEmptyString,
      status: DeliveryStatus,
      updatedAt: Option[ZonedDateTime] = None,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.SmsMessage =
      this.transformInto[domain.SmsMessage]
  }

  case class VehicleCategory(
      id: VehicleCategoryId,
      name: NonEmptyString,
      vehicleType: VehicleType,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.VehicleCategory =
      this.transformInto[domain.VehicleCategory]
  }

  case class Vehicle(
      id: VehicleId,
      createdAt: ZonedDateTime,
      vehicleType: VehicleType,
      registeredNumber: Option[NonEmptyString],
      brand: NonEmptyString,
      color: Option[NonEmptyString],
      owner: Option[NonEmptyString],
      address: Option[NonEmptyString],
      dateOfIssue: Option[LocalDate],
      issuingAuthority: Option[NonEmptyString],
      pin: Option[NonNegInt],
      yearOfRelease: NonNegInt,
      vehicleCategoryId: VehicleCategoryId,
      bodyNumber: Option[NonEmptyString],
      chassisNumber: Option[NonEmptyString],
      maxMass: NonNegInt,
      unloadMass: NonNegInt,
      engineNumber: Option[NonEmptyString],
      engineCapacity: Option[NonNegInt],
      numberOfSeats: NonNegInt,
      numberOfStandingPlaces: NonNegInt,
      specialMarks: Option[NonEmptyString],
      licenseNumber: Option[NonEmptyString],
      branchId: BranchId,
      inventoryNumber: InventoryNumber,
      conditionType: ConditionType,
      gpsTracking: Option[GpsTrackingType],
      fuelLevelSensor: Option[NonNegDouble],
      description: Option[NonEmptyString],
      deleted: Boolean = false,
    ) {
    def toDomain(
        branch: Option[domain.Branch],
        vehicleCategory: domain.VehicleCategory,
        fuels: List[domain.VehicleFuelItem],
        vehiclePhotos: List[AssetId],
        vehicleLicensePhotos: List[AssetId],
      ): domain.Vehicle =
      this
        .into[domain.Vehicle]
        .withFieldConst(_.branch, branch)
        .withFieldConst(_.vehicleCategory, vehicleCategory.some)
        .withFieldConst(_.fuels, fuels)
        .withFieldConst(_.vehiclePhotos, vehiclePhotos)
        .withFieldConst(_.vehicleLicensePhotos, vehicleLicensePhotos)
        .transform
  }

  case class VehicleFuelItem(
      id: VehicleFuelItemId,
      vehicleId: VehicleId,
      fuelType: FuelType,
      fuelTankVolume: NonNegDouble,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.VehicleFuelItem =
      this
        .into[domain.VehicleFuelItem]
        .transform
  }

  case class VehiclePhoto(
      id: VehiclePhotoId,
      vehicleId: VehicleId,
      assetId: AssetId,
      deleted: Boolean = false,
    )

  case class VehicleLicensePhoto(
      id: VehicleLicensePhotoId,
      vehicleId: VehicleId,
      assetId: AssetId,
      deleted: Boolean = false,
    )

  case class MedicalExamination(
      id: MedicalExaminationId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      driverId: UserId,
      driverPersonalNumber: NonNegInt,
      complaint: Option[NonEmptyString],
      pulse: NonNegInt,
      bodyTemperature: NonNegDouble,
      bloodPressure: NonEmptyString,
      alcoholConcentration: NonNegDouble,
      driverHealth: HealthType,
      doctorId: UserId,
      doctorSignature: AssetId,
      deleted: Boolean = false,
    )

  case class Trip(
      id: TripId,
      createdAt: ZonedDateTime,
      startDate: LocalDate,
      endDate: Option[LocalDate],
      serialNumber: Option[NonEmptyString],
      firstTab: Option[NonEmptyString],
      secondTab: Option[NonEmptyString],
      thirdTab: Option[NonEmptyString],
      workingMode: Option[WorkingModeType],
      summation: Option[NonEmptyString],
      vehicleId: VehicleId,
      notes: Option[NonEmptyString],
      deleted: Boolean = false,
    ) {
    def toDomain(
        vehicle: Option[domain.Vehicle],
        drivers: List[domain.TripDriver],
        trailer: Option[List[domain.Vehicle]],
        semiTrailer: Option[List[domain.Vehicle]],
        accompanyingPersons: Option[List[AuthedUser.User]],
      ): domain.Trip =
      this
        .into[domain.Trip]
        .withFieldConst(_.vehicle, vehicle)
        .withFieldConst(_.drivers, drivers)
        .withFieldConst(_.trailer, trailer)
        .withFieldConst(_.semiTrailer, semiTrailer)
        .withFieldConst(_.accompanyingPersons, accompanyingPersons)
        .transform
  }

  case class TripDriver(
      id: TripDriverId,
      tripId: TripId,
      driverId: UserId,
      drivingLicenseNumber: NonEmptyString,
      driverHealth: Option[HealthType] = None,
      doctorId: Option[UserId] = None,
      doctorSignature: Option[AssetId] = None,
      medicalExaminationId: Option[MedicalExaminationId] = None,
      deleted: Boolean = false,
    ) {
    def toDomain(
        driver: Option[domain.AuthedUser.User] = None,
        doctor: Option[domain.AuthedUser.User] = None,
      ): domain.TripDriver =
      this
        .into[domain.TripDriver]
        .withFieldConst(_.driver, driver)
        .withFieldConst(_.doctor, doctor)
        .transform
  }

  case class TripAccompanyingPerson(
      id: TripAccompanyingPersonId,
      tripId: TripId,
      userId: UserId,
      deleted: Boolean = false,
    )

  case class TripTrailer(
      id: TripTrailerId,
      tripId: TripId,
      trailerId: VehicleId,
      deleted: Boolean = false,
    )

  case class TripSemiTrailer(
      id: TripSemiTrailerId,
      tripId: TripId,
      semiTrailerId: VehicleId,
      deleted: Boolean = false,
    )

  case class TripVehicleIndicator(
      id: TripVehicleIndicatorId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      vehicleId: VehicleId,
      actionType: VehicleIndicatorActionType,
      scheduledTime: ZonedDateTime,
      currentDateTime: ZonedDateTime,
      odometerIndicator: NonNegDouble,
      spentHours: Option[NonNegDouble],
      paidDistance: NonNegDouble,
      deleted: Boolean = false,
    )

  case class TripVehicleAcceptance(
      id: TripVehicleAcceptanceId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      vehicleId: VehicleId,
      actionType: VehicleIndicatorActionType,
      conditionType: ConditionType,
      mechanicId: Option[UserId],
      mechanicSignature: Option[AssetId],
      driverId: Option[UserId],
      driverSignature: Option[AssetId],
      deleted: Boolean = false,
    )

  case class TripGivenFuel(
      id: TripGivenFuelId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      vehicleId: VehicleId,
      fuelBrand: FuelType,
      brandCode: Option[NonEmptyString],
      fuelGiven: NonNegDouble,
      refuelerId: UserId,
      refuelerSignature: AssetId,
      deleted: Boolean = false,
    )

  case class TripFuelInspection(
      id: TripFuelInspectionId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      vehicleId: VehicleId,
      actionType: VehicleIndicatorActionType,
      mechanicId: UserId,
      mechanicSignature: AssetId,
      deleted: Boolean = false,
    )

  case class TripFuelInspectionItem(
      id: TripFuelInspectionItemId,
      tripFuelInspectionId: TripFuelInspectionId,
      fuelType: FuelType,
      fuelInTank: NonNegDouble,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.TripFuelInspectionItem =
      this
        .into[domain.TripFuelInspectionItem]
        .transform
  }

  case class TripFuelSupply(
      id: TripFuelSupplyId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      vehicleId: VehicleId,
      dispatcherId: UserId,
      dispatcherSignature: AssetId,
      deleted: Boolean = false,
    )

  case class TripFuelSupplyItem(
      id: TripFuelSupplyItemId,
      tripFuelSupplyId: TripFuelSupplyId,
      fuelType: FuelType,
      fuelSupply: NonNegDouble,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.TripFuelSupplyItem =
      this
        .into[domain.TripFuelSupplyItem]
        .transform
  }

  case class TripFuelRate(
      id: TripFuelRateId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      fuelType: FuelType,
      normChangeCoefficient: NonNegDouble,
      equipmentWorkingTime: NonNegDouble,
      engineWorkingTime: NonNegDouble,
      dispatcherId: UserId,
      dispatcherSignature: AssetId,
      deleted: Boolean = false,
    )

  case class TripDriverTask(
      id: TripDriverTaskId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      whoseDiscretion: NonEmptyString,
      arrivalTime: Option[ZonedDateTime],
      pickupLocation: NonEmptyString,
      deliveryLocation: NonEmptyString,
      freightName: NonEmptyString,
      numberOfInteractions: Option[NonNegInt],
      distance: Option[NonNegDouble],
      freightVolume: Option[NonNegDouble],
      deleted: Boolean = false,
    ) {
    def toDomain: domain.TripDriverTask =
      this.transformInto[domain.TripDriverTask]
  }

  case class TripRouteDelay(
      id: TripRouteDelayId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      name: NonEmptyString,
      startTime: ZonedDateTime,
      endTime: ZonedDateTime,
      userId: UserId,
      userSignature: AssetId,
      deleted: Boolean = false,
    )

  case class TripCompleteTask(
      id: TripCompleteTaskId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      commuteNumber: NonNegInt,
      loadNumbers: NonEmptyString,
      arrivalTime: ZonedDateTime,
      consignorFullName: NonEmptyString,
      consignorSignature: AssetId,
      driverId: UserId,
      deleted: Boolean = false,
    )

  case class TripCompleteTaskAcceptance(
      id: TripCompleteTaskAcceptanceId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      commuteNumberTotal: NonNegInt,
      loadNumberTotal: NonNegInt,
      loadNumberTotalStr: NonEmptyString,
      documentId: Option[AssetId],
      driverId: Option[UserId],
      driverSignature: Option[AssetId],
      dispatcherId: Option[UserId],
      dispatcherSignature: Option[AssetId],
      deleted: Boolean = false,
    )

  case class VehicleHistory(
      id: VehicleHistoryId,
      createdAt: ZonedDateTime,
      vehicleId: VehicleId,
      branchId: BranchId,
      registeredNumber: Option[RegisteredNumber],
    ) {
    def toDomain(
        vehicleCategory: domain.VehicleCategory,
        branch: Option[domain.Branch],
      ): domain.VehicleHistory =
      this
        .into[domain.VehicleHistory]
        .withFieldConst(_.vehicleCategory, vehicleCategory)
        .withFieldConst(_.branch, branch)
        .transform
  }

  case class VehicleHistoryWithCategory(
      id: VehicleHistoryId,
      createdAt: ZonedDateTime,
      vehicleCategoryId: VehicleCategoryId,
      vehicleCategoryName: NonEmptyString,
      vehicleCategoryType: VehicleType,
      registeredNumber: Option[RegisteredNumber],
    )
}
