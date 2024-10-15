package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait VehicleType extends Snakecase

object VehicleType extends CirceEnum[VehicleType] with Enum[VehicleType] {
  case object Truck extends VehicleType
  case object Bus extends VehicleType
  case object Auto extends VehicleType
  case object Pickup extends VehicleType
  case object Trailer extends VehicleType
  case object RoadConstructionVehicle extends VehicleType
  override def values: IndexedSeq[VehicleType] = findValues
}
