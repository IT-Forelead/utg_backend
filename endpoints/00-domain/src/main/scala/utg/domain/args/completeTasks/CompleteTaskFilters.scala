package utg.domain.args.completeTasks

import eu.timepit.refined.types.numeric.PosInt
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class CompleteTaskFilters(
    limit: Option[PosInt] = None,
    page: Option[PosInt] = None,
  )
