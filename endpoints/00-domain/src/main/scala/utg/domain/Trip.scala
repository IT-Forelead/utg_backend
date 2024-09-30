package utg.domain

import java.time.LocalDate
import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User
import utg.domain.enums.WorkingModeType

@JsonCodec
case class Trip(
    id: TripId,
    createdAt: ZonedDateTime,
    startDate: LocalDate,
    endDate: Option[LocalDate],
    serialNumber: Option[NonEmptyString],
    firstTab: Option[NonEmptyString],
    secondTab: Option[NonEmptyString],
    thirdTab: Option[NonEmptyString],
    workingMode: Option[WorkingModeType],
    summation: Option[NonEmptyString],
    vehicle: Option[Vehicle],
    drivers: List[TripDriver],
    trailer: Option[List[Vehicle]],
    semiTrailer: Option[List[Vehicle]],
    accompanyingPersons: Option[List[User]],
    notes: Option[NonEmptyString],
  )
