package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait StatusType extends Snakecase

object StatusType extends CirceEnum[StatusType] with Enum[StatusType] {
  case object New extends StatusType
  case object InProgress extends StatusType
  case object Completed extends StatusType
  case object Closed extends StatusType
  override def values: IndexedSeq[StatusType] = findValues
}


