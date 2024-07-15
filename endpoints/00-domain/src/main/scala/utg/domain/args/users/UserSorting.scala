package utg.domain.args.users

import scala.collection.immutable

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait UserSorting extends Snakecase
object UserSorting extends Enum[UserSorting] with CirceEnum[UserSorting] {
  final case object CreatedAt extends UserSorting
  final case object FirstName extends UserSorting
  final case object LastName extends UserSorting
  final case object Role extends UserSorting

  override def values: immutable.IndexedSeq[UserSorting] = findValues
}
