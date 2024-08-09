package utg.domain.args.tripVehicleAcceptances

import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.enums.ConditionType
import utg.domain.enums.VehicleIndicatorActionType

@JsonCodec
case class TripVehicleAcceptanceInput(
    tripId: TripId,
    actionType: VehicleIndicatorActionType,
    conditionType: ConditionType,
    mechanicId: Option[UserId],
    mechanicSignature: Option[AssetId],
    driverId: Option[UserId],
    driverSignature: Option[AssetId],
  )
