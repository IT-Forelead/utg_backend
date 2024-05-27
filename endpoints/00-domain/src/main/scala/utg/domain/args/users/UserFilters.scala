package utg.domain.args.users

import eu.timepit.refined.types.numeric.PosInt

import utg.domain.UserId

case class UserFilters(
    id: Option[UserId] = None,
    limit: Option[PosInt] = None,
    offset: Option[PosInt] = None,
  )
