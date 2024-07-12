package utg.domain.args.branches

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.BranchId
import utg.domain.RegionId

@JsonCodec
case class UpdateBranchInput(
    id: BranchId,
    name: NonEmptyString,
    code: NonEmptyString,
    regionId: RegionId,
  )
