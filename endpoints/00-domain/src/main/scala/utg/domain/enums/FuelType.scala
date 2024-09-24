package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait FuelType extends Snakecase

object FuelType extends CirceEnum[FuelType] with Enum[FuelType] {
  case object Petrol extends FuelType
  case object Diesel extends FuelType
  case object Methane extends FuelType
  case object Propane extends FuelType
  case object Hybrid extends FuelType
  case object Electric extends FuelType
  case object Kerosene extends FuelType
  override def values: IndexedSeq[FuelType] = findValues
}
