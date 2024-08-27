package utg.domain.args.trips

import java.time.LocalDate

import cats.data.NonEmptyList
import eu.timepit.refined.types.numeric.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.VehicleId
import utg.domain.enums.WorkingModeType

@JsonCodec
case class UpdateTripInput(
    id: TripId,
    startDate: Option[LocalDate],
    endDate: Option[LocalDate],
    serialNumber: Option[NonEmptyString],
    firstTab: Option[NonEmptyString],
    secondTab: Option[NonEmptyString],
    thirdTab: Option[NonEmptyString],
    workingMode: Option[WorkingModeType],
    summation: Option[NonEmptyString],
    driverIds: Option[NonEmptyList[UserId]],
    vehicleId: Option[VehicleId],
    trailerIds: Option[NonEmptyList[VehicleId]],
    semiTrailerIds: Option[NonEmptyList[VehicleId]],
    accompanyingPersonIds: Option[NonEmptyList[UserId]],
    doctorId: Option[UserId],
    doctorSignature: Option[AssetId],
    fuelSupply: Option[NonNegDouble],
    chiefMechanicId: Option[UserId],
    chiefMechanicSignature: Option[AssetId],
    notes: Option[NonEmptyString],
  )
