package utg.domain.args.trips

import eu.timepit.refined.types.numeric.NonNegDouble
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.UserId

@JsonCodec
case class TripChiefMechanicInput(
    tripId: TripId,
    fuelSupply: Option[NonNegDouble],
    chiefMechanicId: Option[UserId],
    chiefMechanicSignature: Option[AssetId],
  )
