package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait VehicleIndicatorType extends Snakecase

object VehicleIndicatorType extends CirceEnum[VehicleIndicatorType] with Enum[VehicleIndicatorType] {
  case object Enter extends VehicleIndicatorType
  case object Exit extends VehicleIndicatorType
  override def values: IndexedSeq[VehicleIndicatorType] = findValues
}
