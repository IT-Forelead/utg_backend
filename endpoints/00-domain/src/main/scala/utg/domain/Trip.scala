package utg.domain

import java.time.LocalDate
import java.time.ZonedDateTime

import eu.timepit.refined.types.numeric.NonNegDouble
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
    drivers: List[TripDriver],
    trailer: Option[Vehicle],
    semiTrailer: Option[Vehicle],
    accompanyingPersons: Option[List[User]],
    doctor: Option[User],
    doctorSignature: Option[AssetId],
    fuelSupply: Option[NonNegDouble],
    chiefMechanic: Option[User],
    chiefMechanicSignature: Option[AssetId],
    notes: Option[NonEmptyString],
  )
