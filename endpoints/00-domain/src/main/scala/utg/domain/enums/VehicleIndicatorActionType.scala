package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait VehicleIndicatorActionType extends Snakecase

object VehicleIndicatorActionType
    extends CirceEnum[VehicleIndicatorActionType]
       with Enum[VehicleIndicatorActionType] {
  case object Exit extends VehicleIndicatorActionType
  case object Back extends VehicleIndicatorActionType
  override def values: IndexedSeq[VehicleIndicatorActionType] = findValues
}
