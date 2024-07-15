package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait GpsTrackerType extends Snakecase

object GpsTrackerType extends CirceEnum[GpsTrackerType] with Enum[GpsTrackerType] {
  case object NotInstalled extends GpsTrackerType
  case object Installed extends GpsTrackerType
  case object Enabled extends GpsTrackerType
  case object Disabled extends GpsTrackerType
  override def values: IndexedSeq[GpsTrackerType] = findValues
}
