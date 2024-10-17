package utg.domain.args.trips

import java.time.ZonedDateTime

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.TripId
import utg.domain.UserId
import utg.domain.VehicleId
import utg.domain.enums.WorkingModeType

@JsonCodec
case class UpdateTripInput(
    id: TripId,
    startDate: Option[ZonedDateTime],
    endDate: Option[ZonedDateTime],
    serialNumber: Option[NonEmptyString],
    firstTab: Option[NonEmptyString],
    secondTab: Option[NonEmptyString],
    workingMode: Option[WorkingModeType],
    summation: Option[NonEmptyString],
    driverIds: Option[NonEmptyList[UserId]],
    vehicleId: Option[VehicleId],
    trailerIds: Option[NonEmptyList[VehicleId]],
    semiTrailerIds: Option[NonEmptyList[VehicleId]],
    accompanyingPersonIds: Option[NonEmptyList[UserId]],
    notes: Option[NonEmptyString],
  )
