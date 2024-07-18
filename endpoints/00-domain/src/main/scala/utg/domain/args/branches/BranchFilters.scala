package utg.domain.args.branches

import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class BranchFilters(
    name: Option[NonEmptyString] = None,
    code: Option[NonEmptyString] = None,
    limit: Option[PosInt] = None,
    page: Option[PosInt] = None,
  )
