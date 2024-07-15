package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait ConditionType extends Snakecase

object ConditionType extends CirceEnum[ConditionType] with Enum[ConditionType] {
  case object Valid extends ConditionType
  case object Invalid extends ConditionType
  case object WriteOff extends ConditionType
  override def values: IndexedSeq[ConditionType] = findValues
}
