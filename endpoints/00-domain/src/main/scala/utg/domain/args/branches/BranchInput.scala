package utg.domain.args.branches

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.RegionId

@JsonCodec
case class BranchInput(
    name: NonEmptyString,
    regionId: RegionId,
  )
