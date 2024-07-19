package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait VehicleIndicatorActionType extends Snakecase

object VehicleIndicatorActionType
    extends CirceEnum[VehicleIndicatorActionType]
       with Enum[VehicleIndicatorActionType] {
  case object Enter extends VehicleIndicatorActionType
  case object Exit extends VehicleIndicatorActionType
  override def values: IndexedSeq[VehicleIndicatorActionType] = findValues
}
