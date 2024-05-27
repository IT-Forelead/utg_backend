package utg.repos.sql

import utg.domain.enums.Privilege
import eu.timepit.refined.types.string.NonEmptyString
import skunk._
import skunk.implicits._
import utg.domain.RoleId

private[repos] object RolesSql extends Sql[RoleId] {
  private[repos] val codec = (id *: nes).to[dto.Role]

  val getById: Query[RoleId, NonEmptyString *: Privilege *: EmptyTuple] =
    sql"""SELECT r.name, rp.privilege FROM role_privileges rp
          join roles r on r.id = rp.role_id
          WHERE role_id = $id""".query(nes *: privilege)

  def getByIds(
      ids: List[RoleId]
    ): Query[ids.type, RoleId *: NonEmptyString *: Privilege *: EmptyTuple] =
    sql"""SELECT r.id, r.name, rp.privilege FROM role_privileges rp
          join roles r on r.id = rp.role_id
          WHERE role_id IN (${id.values.list(ids)})""".query(id *: nes *: privilege)
}