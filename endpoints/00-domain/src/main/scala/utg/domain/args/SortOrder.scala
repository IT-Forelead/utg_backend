package utg.domain.args

import scala.collection.immutable

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait SortOrder extends EnumEntry with Snakecase {
  def value: String
}

object SortOrder extends CirceEnum[SortOrder] with Enum[SortOrder] {
  final case object Ascending extends SortOrder {
    override def value: String = "ASC"
  }
  final case object Descending extends SortOrder {
    override def value: String = "DESC"
  }
  override def values: immutable.IndexedSeq[SortOrder] = findValues
}
