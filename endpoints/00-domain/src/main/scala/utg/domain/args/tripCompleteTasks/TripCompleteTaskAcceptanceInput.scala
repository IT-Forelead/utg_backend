package utg.domain.args.tripCompleteTasks

import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.UserId

@JsonCodec
case class TripCompleteTaskAcceptanceInput(
    tripId: TripId,
    commuteNumberTotal: Option[NonNegInt],
    loadNumberTotal: Option[NonNegInt],
    loadNumberTotalStr: Option[NonEmptyString],
    documentId: Option[AssetId],
    driverId: Option[UserId],
    driverSignature: Option[AssetId],
    dispatcherId: Option[UserId],
    dispatcherSignature: Option[AssetId],
    isDispatcher: Boolean,
  )
