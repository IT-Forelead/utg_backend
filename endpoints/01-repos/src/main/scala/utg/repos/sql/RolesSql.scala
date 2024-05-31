package utg.repos.sql

import eu.timepit.refined.types.string.NonEmptyString
import skunk._
import skunk.implicits._

import utg.domain.RoleId
import utg.domain.enums.Privilege

private[repos] object RolesSql extends Sql[RoleId] {
  private[repos] val codec = (id *: nes).to[dto.Role]

  val getById: Query[RoleId, NonEmptyString *: Option[Privilege] *: EmptyTuple] =
    sql"""SELECT r.name, rp.privilege FROM roles r
          LEFT JOIN role_privileges rp on r.id = rp.role_id
          WHERE r.id = $id""".query(nes *: privilege.opt)

  def getByIds(
      ids: List[RoleId]
    ): Query[ids.type, RoleId *: NonEmptyString *: Option[Privilege] *: EmptyTuple] =
    sql"""SELECT r.id, r.name, rp.privilege FROM roles r
          LEFT JOIN role_privileges rp on r.id = rp.role_id
          WHERE r.id IN (${id.values.list(ids)})""".query(id *: nes *: privilege.opt)

  val getAll: Query[Void, dto.Role] =
    sql""" SELECT * FROM roles""".query(codec)

  val getAllPrivileges: Query[Void, RoleId *: Privilege *: EmptyTuple] =
    sql"""SELECT * FROM role_privileges""".query(id *: privilege)
}
