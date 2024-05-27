package utg.repos

import java.time.ZonedDateTime

import enumeratum.Enum
import enumeratum.EnumEntry
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Codec
import skunk.codec.all._
import skunk.data.Arr
import skunk.data.Type
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.Phone
import utg.domain.enums.Privilege
import utg.effects.IsUUID

package object sql {
  def identification[A: IsUUID]: Codec[A] = uuid.imap[A](IsUUID[A].uuid.get)(IsUUID[A].uuid.apply)

  private def _enum[A <: EnumEntry](`enum`: Enum[A], tpe: Type): Codec[Arr[A]] =
    Codec.array[A](
      _.entryName,
      s => `enum`.withNameOption(s).toRight(s"${`enum`}: no such element '$s'"),
      tpe,
    )
  val nes: Codec[NonEmptyString] = varchar.imap[NonEmptyString](identity(_))(_.value)
  val nonEmptyText: Codec[NonEmptyString] = text.imap[NonEmptyString](identity(_))(_.value)
  val phone: Codec[Phone] = varchar.imap[Phone](identity(_))(_.value)
  val privilege: Codec[Privilege] = varchar.imap[Privilege](Privilege.withName)(_.entryName)
  val zonedDateTime: Codec[ZonedDateTime] = timestamptz.imap(_.toZonedDateTime)(_.toOffsetDateTime)

  val passwordHash: Codec[PasswordHash[SCrypt]] =
    varchar.imap[PasswordHash[SCrypt]](PasswordHash[SCrypt])(identity)
}
