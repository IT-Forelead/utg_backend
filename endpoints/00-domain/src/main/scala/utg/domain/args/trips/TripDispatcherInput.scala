package utg.domain.args.trips

import java.time.LocalDate

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.TripId
import utg.domain.UserId
import utg.domain.VehicleId
import utg.domain.enums.WorkingModeType

@JsonCodec
case class TripDispatcherInput(
    tripId: TripId,
    startDate: LocalDate,
    endDate: Option[LocalDate],
    serialNumber: NonEmptyString,
    firstTab: Option[NonEmptyString],
    secondTab: Option[NonEmptyString],
    thirdTab: Option[NonEmptyString],
    workingMode: WorkingModeType,
    summation: Option[NonEmptyString],
    vehicleId: VehicleId,
    driverId: UserId,
    trailerId: Option[VehicleId],
    semiTrailerId: Option[VehicleId],
    accompanyingPersons: Option[NonEmptyList[UserId]],
  )
