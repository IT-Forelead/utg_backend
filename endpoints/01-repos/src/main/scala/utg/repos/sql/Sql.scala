package utg.repos.sql

import skunk.Codec
import utg.effects.IsUUID

abstract class Sql[T: IsUUID] {
  val id: Codec[T] = identification[T]
}
