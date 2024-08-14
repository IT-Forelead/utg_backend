package utg.domain.enums

import enumeratum.EnumEntry.{Snakecase, UpperSnakecase, UpperWords}
import enumeratum._
import eu.timepit.refined.char.UpperCase

sealed trait VehicleType extends Snakecase

object VehicleType extends CirceEnum[VehicleType] with Enum[VehicleType] {
  case object Auto extends VehicleType
  case object SpecialRoadVehicles extends VehicleType
  case object Trailer extends VehicleType
  case object WeldingEquipment extends VehicleType
  case object OtherMechanism extends VehicleType
  override def values: IndexedSeq[VehicleType] = findValues
}
