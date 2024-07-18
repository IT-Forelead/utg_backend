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
    serialNumber: NonEmptyString,
    firstTab: Option[NonEmptyString],
    secondTab: Option[NonEmptyString],
    thirdTab: Option[NonEmptyString],
    workingMode: WorkingModeType,
    summation: Option[NonEmptyString],
    vehicle: Option[Vehicle],
    driver: Option[User],
    trailer: Option[Vehicle],
    semiTrailer: Option[Vehicle],
    accompanyingPersons: Option[List[User]],
  )
