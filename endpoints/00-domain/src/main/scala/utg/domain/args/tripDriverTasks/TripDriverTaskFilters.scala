package utg.domain.args.tripDriverTasks

import eu.timepit.refined.types.numeric.PosInt
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class TripDriverTaskFilters(
    limit: Option[PosInt] = None,
    page: Option[PosInt] = None,
  )
