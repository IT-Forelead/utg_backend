package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User
import utg.domain.enums._

@JsonCodec
case class MedicalExamination(
    id: MedicalExaminationId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    driver: Option[User],
    driverPersonalNumber: NonNegInt,
    complaint: Option[NonEmptyString],
    pulse: NonNegInt,
    bodyTemperature: NonNegDouble,
    bloodPressure: NonEmptyString,
    alcoholConcentration: NonNegDouble,
    driverHealth: HealthType,
    doctor: Option[User],
    doctorSignature: AssetId,
  )
