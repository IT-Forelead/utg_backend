package utg.repos

import java.time.ZonedDateTime

import enumeratum.Enum
import enumeratum.EnumEntry
import eu.timepit.refined.types.numeric._
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Codec
import skunk.codec.all._
import skunk.data.Arr
import skunk.data.Type
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg._
import utg.domain.DocumentId
import utg.domain.enums._
import utg.effects.IsUUID

package object sql {
  def identification[A: IsUUID]: Codec[A] = uuid.imap[A](IsUUID[A].uuid.get)(IsUUID[A].uuid.apply)

  private def _enum[A <: EnumEntry](`enum`: Enum[A], tpe: Type): Codec[Arr[A]] =
    Codec.array[A](
      _.entryName,
      s => `enum`.withNameOption(s).toRight(s"${`enum`}: no such element '$s'"),
      tpe,
    )
  val nes: Codec[NonEmptyString] = varchar.imap[NonEmptyString](identity(_))(_.value)
  val nonEmptyText: Codec[NonEmptyString] = text.imap[NonEmptyString](identity(_))(_.value)
  val phone: Codec[Phone] = varchar.imap[Phone](identity(_))(_.value)
  val registeredNumber: Codec[RegisteredNumber] =
    varchar.imap[RegisteredNumber](identity(_))(_.value)
  val inventoryNumber: Codec[InventoryNumber] =
    varchar.imap[InventoryNumber](identity(_))(_.value)
  val privilege: Codec[Privilege] = varchar.imap[Privilege](Privilege.withName)(_.entryName)
  val zonedDateTime: Codec[ZonedDateTime] = timestamptz.imap(_.toZonedDateTime)(_.toOffsetDateTime)
  val nonNegDouble: Codec[NonNegDouble] =
    float8.imap[NonNegDouble](double => NonNegDouble.unsafeFrom(double))(_.value)
  val nonNegInt: Codec[NonNegInt] =
    int4.imap[NonNegInt](int => NonNegInt.unsafeFrom(int))(_.value)
  val vehicleType: Codec[VehicleType] =
    `enum`[VehicleType](VehicleType, Type("vehicle_type"))
  val conditionType: Codec[ConditionType] =
    `enum`[ConditionType](ConditionType, Type("condition_type"))
  val fuelType: Codec[FuelType] = `enum`[FuelType](FuelType, Type("fuel_type"))
  val gpsTrackingType: Codec[GpsTrackingType] =
    `enum`[GpsTrackingType](GpsTrackingType, Type("gps_tracking_type"))
  val workingModeType: Codec[WorkingModeType] =
    `enum`[WorkingModeType](WorkingModeType, Type("working_mode_type"))
  val vehicleIndicatorActionType: Codec[VehicleIndicatorActionType] =
    `enum`[VehicleIndicatorActionType](
      VehicleIndicatorActionType,
      Type("vehicle_indicator_action_type"),
    )
  val documentId: Codec[DocumentId] = uuid.imap[DocumentId](uuid => DocumentId(uuid))(_.value)

  private val _drivingLicenseCategory: Codec[Arr[DrivingLicenseCategory]] =
    `_enum`[DrivingLicenseCategory](
      DrivingLicenseCategory,
      Type("_driving_license_category", List(Type("driving_license_category"))),
    )
  val drivingLicenseCategories: Codec[List[DrivingLicenseCategory]] =
    _drivingLicenseCategory.imap(_.flattenTo(List))(Arr(_: _*))

  private val _fuelType: Codec[Arr[FuelType]] =
    `_enum`[FuelType](FuelType, Type("_fuel_type", List(Type("fuel_type"))))
  val fuelTypes: Codec[List[FuelType]] =
    _fuelType.imap(_.flattenTo(List))(Arr(_: _*))

  val passwordHash: Codec[PasswordHash[SCrypt]] =
    varchar.imap[PasswordHash[SCrypt]](PasswordHash[SCrypt])(identity)
}
