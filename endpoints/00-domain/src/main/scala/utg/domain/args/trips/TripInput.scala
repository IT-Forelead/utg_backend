package utg.domain.args.trips

import java.time.LocalDate

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.UserId
import utg.domain.VehicleId
import utg.domain.enums.WorkingModeType

@JsonCodec
case class TripInput(
    startDate: LocalDate,
    endDate: Option[LocalDate],
    serialNumber: Option[NonEmptyString],
    firstTab: Option[NonEmptyString],
    secondTab: Option[NonEmptyString],
    thirdTab: Option[NonEmptyString],
    workingMode: WorkingModeType,
    summation: Option[NonEmptyString],
    vehicleId: VehicleId,
    driverIds: NonEmptyList[UserId],
    trailerIds: Option[NonEmptyList[VehicleId]],
    semiTrailerIds: Option[NonEmptyList[VehicleId]],
    accompanyingPersonIds: Option[NonEmptyList[UserId]],
  )
