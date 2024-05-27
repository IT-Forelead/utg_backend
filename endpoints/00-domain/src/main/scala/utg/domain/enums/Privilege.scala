package utg.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait Privilege extends Snakecase {
  val group: String
}
object Privilege extends Enum[Privilege] with CirceEnum[Privilege] {
  case object CreateUser extends Privilege {
    override val group: String = "USER"
  }
  case object UpdateUser extends Privilege {
    override val group: String = "USER"
  }
  case object UpdateAnyUser extends Privilege {
    override val group: String = "USER"
  }
  case object ViewUsers extends Privilege {
    override val group: String = "USER"
  }
  override def values: IndexedSeq[Privilege] = findValues
}
