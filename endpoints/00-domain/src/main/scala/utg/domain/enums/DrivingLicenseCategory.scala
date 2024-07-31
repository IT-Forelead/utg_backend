package utg.domain.enums

import enumeratum.EnumEntry.UpperWords
import enumeratum._

sealed trait DrivingLicenseCategory extends UpperWords

object DrivingLicenseCategory
    extends CirceEnum[DrivingLicenseCategory]
       with Enum[DrivingLicenseCategory] {
  case object A extends DrivingLicenseCategory
  case object B extends DrivingLicenseCategory
  case object C extends DrivingLicenseCategory
  case object D extends DrivingLicenseCategory
  case object BE extends DrivingLicenseCategory
  case object CE extends DrivingLicenseCategory
  case object DE extends DrivingLicenseCategory
  override def values: IndexedSeq[DrivingLicenseCategory] = findValues
}
