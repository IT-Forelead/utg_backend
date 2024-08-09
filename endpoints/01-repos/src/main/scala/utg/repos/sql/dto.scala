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
      personalNumber: NonNegInt,
      phone: Phone,
      roleId: RoleId,
      assetId: Option[AssetId],
      branchCode: Option[NonEmptyString],
      drivingLicenseNumber: Option[NonEmptyString],
      drivingLicenseCategories: Option[List[DrivingLicenseCategory]],
    ) {
    def toDomain(
        role: domain.Role,
        branch: Option[utg.domain.Branch],
        drivingLicenseCategories: Option[NonEmptyList[DrivingLicenseCategory]],
      ): AuthedUser.User =
      this
        .into[AuthedUser.User]
        .withFieldConst(_.role, role)
        .withFieldConst(_.phone, phone)
        .withFieldConst(_.branch, branch)
        .withFieldConst(_.drivingLicenseCategories, drivingLicenseCategories)
        .transform

    def ldtToString(date: ZonedDateTime, format: String = "yyyy MM dd HH:mm"): String =
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

    def writeAsCsv(rows: List[String]): String = {
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
  case class VehicleCategory(
      id: VehicleCategoryId,
      name: NonEmptyString,
      vehicleType: VehicleType,
      deleted: Boolean = false,
    )

  case class Vehicle(
      id: VehicleId,
      createdAt: ZonedDateTime,
      branchId: BranchId,
      vehicleCategoryId: VehicleCategoryId,
      vehicleType: VehicleType,
      brand: NonEmptyString,
      registeredNumber: Option[RegisteredNumber],
      inventoryNumber: InventoryNumber,
      yearOfRelease: NonNegInt,
      bodyNumber: Option[NonEmptyString],
      chassisNumber: Option[NonEmptyString],
      engineNumber: Option[NonEmptyString],
      conditionType: ConditionType,
      fuelTypes: Option[List[FuelType]],
      description: Option[NonEmptyString],
      gpsTracking: Option[GpsTrackingType],
      fuelLevelSensor: Option[NonNegDouble],
      fuelTankVolume: Option[NonNegDouble],
      deleted: Boolean = false,
    ) {
    def toDomain(
        branch: Option[domain.Branch],
        vehicleCategory: domain.VehicleCategory,
        fuelTypes: Option[NonEmptyList[FuelType]],
      ): domain.Vehicle =
      this
        .into[domain.Vehicle]
        .withFieldConst(_.branch, branch)
        .withFieldConst(_.vehicleCategory, vehicleCategory.some)
        .withFieldConst(_.fuelTypes, fuelTypes)
        .transform
  }

  case class Trip(
      id: TripId,
      createdAt: ZonedDateTime,
      startDate: LocalDate,
      endDate: Option[LocalDate],
      serialNumber: NonEmptyString,
      firstTab: Option[NonEmptyString],
      secondTab: Option[NonEmptyString],
      thirdTab: Option[NonEmptyString],
      workingMode: WorkingModeType,
      summation: Option[NonEmptyString],
      vehicleId: VehicleId,
      trailerId: Option[VehicleId],
      semiTrailerId: Option[VehicleId],
      doctorId: Option[UserId],
      doctorSignature: Option[AssetId],
      fuelSupply: Option[NonNegDouble],
      chiefMechanicId: Option[UserId],
      chiefMechanicSignature: Option[AssetId],
      notes: Option[NonEmptyString],
      deleted: Boolean = false,
    ) {
    def toDomain(
        vehicle: Option[domain.Vehicle],
//        driver: Option[domain.AuthedUser.User],
        trailer: Option[domain.Vehicle],
        semiTrailer: Option[domain.Vehicle],
        accompanyingPersons: Option[List[AuthedUser.User]],
        doctor: Option[domain.AuthedUser.User],
        chiefMechanic: Option[domain.AuthedUser.User],
      ): domain.Trip =
      this
        .into[domain.Trip]
        .withFieldConst(_.vehicle, vehicle)
//        .withFieldConst(_.driver, driver)
        .withFieldConst(_.trailer, trailer)
        .withFieldConst(_.semiTrailer, semiTrailer)
        .withFieldConst(_.accompanyingPersons, accompanyingPersons)
        .withFieldConst(_.doctor, doctor)
        .withFieldConst(_.chiefMechanic, chiefMechanic)
        .transform
  }

  case class TripDriver(
      id: TripDriverId,
      tripId: TripId,
      driverId: UserId,
      drivingLicenseNumber: NonEmptyString,
      deleted: Boolean = false,
    )

  case class AccompanyingPerson(
      id: AccompanyingPersonId,
      tripId: TripId,
      userId: UserId,
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

  case class TripFuelExpense(
      id: TripFuelExpenseId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      vehicleId: VehicleId,
      fuelBrand: Option[NonEmptyString],
      brandCode: Option[NonEmptyString],
      fuelGiven: Option[NonNegDouble],
      refuelerId: Option[UserId],
      attendantSignature: Option[AssetId],
      fuelInTank: Option[NonNegDouble],
      fuelRemaining: Option[NonNegDouble],
      normChangeCoefficient: Option[NonNegDouble],
      equipmentWorkingTime: Option[NonNegDouble],
      engineWorkingTime: Option[NonNegDouble],
      tankCheckMechanicId: Option[UserId],
      tankCheckMechanicSignature: Option[AssetId],
      remainingCheckMechanicId: Option[UserId],
      remainingCheckMechanicSignature: Option[AssetId],
      dispatcherId: Option[UserId],
      dispatcherSignature: Option[AssetId],
      deleted: Boolean = false,
    )

  case class TripDriverTask(
      id: TripDriverTaskId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      whoseDiscretion: NonEmptyString,
      arrivalTime: ZonedDateTime,
      pickupLocation: NonEmptyString,
      deliveryLocation: NonEmptyString,
      freightName: NonEmptyString,
      numberOfInteractions: NonNegInt,
      distance: NonNegDouble,
      freightVolume: NonNegDouble,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.TripDriverTask =
      this.transformInto[domain.TripDriverTask]
  }

  case class LineDelay(
      id: LineDelayId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      name: NonEmptyString,
      startTime: ZonedDateTime,
      endTime: ZonedDateTime,
      signId: SignId,
      deleted: Boolean = false,
    ) {
    def toDomain: domain.LineDelay =
      this.transformInto[domain.LineDelay]
  }

  case class CompleteTask(
      id: CompleteTaskId,
      createdAt: ZonedDateTime,
      tripId: TripId,
      tripNumber: Option[NonEmptyString],
      invoiceNumber: Option[NonEmptyString],
      arrivalTime: Option[ZonedDateTime],
      consignorSignId: Option[ConsignorSignId],
      documentId: Option[DocumentId],
      deleted: Boolean = false,
    ) {
    def toDomain: domain.CompleteTask =
      this.transformInto[domain.CompleteTask]
  }
}
