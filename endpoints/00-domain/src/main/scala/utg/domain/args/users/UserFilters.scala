package utg.domain.args.users

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.RoleId
import utg.domain.UserId
import utg.domain.args.SortOrder

@JsonCodec
case class UserFilters(
    id: Option[UserId] = None,
    roleId: Option[RoleId] = None,
    name: Option[NonEmptyString] = None,
    limit: Option[PosInt] = None,
    page: Option[PosInt] = None,
    sortBy: Option[UserSorting] = None,
    sortOrder: Option[SortOrder] = None,
  )
