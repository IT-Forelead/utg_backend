package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait DeliveryStatus extends Snakecase

object DeliveryStatus extends CirceEnum[DeliveryStatus] with Enum[DeliveryStatus] {
  case object Sent extends DeliveryStatus
  case object Delivered extends DeliveryStatus
  case object NotDelivered extends DeliveryStatus
  case object Failed extends DeliveryStatus
  case object Transmitted extends DeliveryStatus
  case object Undefined extends DeliveryStatus
  override def values: IndexedSeq[DeliveryStatus] = findValues
}
