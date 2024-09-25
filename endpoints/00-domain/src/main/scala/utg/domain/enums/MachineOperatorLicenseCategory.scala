package utg.domain.enums

import enumeratum.EnumEntry.Uppercase
import enumeratum._

sealed trait MachineOperatorLicenseCategory extends Uppercase

object MachineOperatorLicenseCategory
  extends CirceEnum[MachineOperatorLicenseCategory]
    with Enum[MachineOperatorLicenseCategory] {
  case object A extends MachineOperatorLicenseCategory
  case object B extends MachineOperatorLicenseCategory
  case object C extends MachineOperatorLicenseCategory
  case object D extends MachineOperatorLicenseCategory
  case object E extends MachineOperatorLicenseCategory
  case object F extends MachineOperatorLicenseCategory
  override def values: IndexedSeq[MachineOperatorLicenseCategory] = findValues
}
