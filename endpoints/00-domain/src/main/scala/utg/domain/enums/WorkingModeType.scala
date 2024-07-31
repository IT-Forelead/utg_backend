package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait WorkingModeType extends Snakecase

object WorkingModeType extends CirceEnum[WorkingModeType] with Enum[WorkingModeType] {
  case object Daily extends WorkingModeType
  case object BusinessTrip extends WorkingModeType
  case object Mixed extends WorkingModeType
  override def values: IndexedSeq[WorkingModeType] = findValues
}
