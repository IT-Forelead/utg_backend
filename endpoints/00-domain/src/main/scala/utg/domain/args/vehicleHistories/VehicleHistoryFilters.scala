package utg.domain.args.vehicleHistories

import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.RegisteredNumber

@JsonCodec
case class VehicleHistoryFilters(
    registeredNumber: Option[RegisteredNumber] = None,
    limit: Option[NonNegInt] = None,
    page: Option[NonNegInt] = None,
  )
