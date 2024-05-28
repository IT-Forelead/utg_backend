package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait VehicleType extends Snakecase

object VehicleType extends Enum[VehicleType] with CirceEnum[VehicleType] {
  case object Car extends VehicleType
  case object Bus extends VehicleType
  case object Truck extends VehicleType
  override def values: IndexedSeq[VehicleType] = findValues
}
