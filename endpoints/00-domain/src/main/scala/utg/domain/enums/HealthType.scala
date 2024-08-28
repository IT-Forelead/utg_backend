package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait HealthType extends Snakecase

object HealthType extends CirceEnum[HealthType] with Enum[HealthType] {
  case object Healthy extends HealthType
  case object Unhealthy extends HealthType
  override def values: IndexedSeq[HealthType] = findValues
}
