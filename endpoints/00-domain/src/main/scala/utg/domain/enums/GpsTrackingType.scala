package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait GpsTrackingType extends Snakecase

object GpsTrackingType extends CirceEnum[GpsTrackingType] with Enum[GpsTrackingType] {
  case object NotInstalled extends GpsTrackingType
  case object Installed extends GpsTrackingType
  case object Enabled extends GpsTrackingType
  case object Disabled extends GpsTrackingType
  override def values: IndexedSeq[GpsTrackingType] = findValues
}
