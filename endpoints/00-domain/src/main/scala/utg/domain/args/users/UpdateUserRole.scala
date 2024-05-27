package utg.domain.args.users

import utg.domain.RoleId
import utg.domain.UserId

case class UpdateUserRole(
    userId: UserId,
    roleId: RoleId,
  )
