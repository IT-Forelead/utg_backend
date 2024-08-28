package utg.domain.args.medicalExaminations

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.enums.HealthType

@JsonCodec
case class MedicalExaminationInput(
    tripId: TripId,
    driverId: UserId,
    complaint: Option[NonEmptyString],
    pulse: NonNegInt,
    bodyTemperature: NonNegDouble,
    bloodPressure: NonEmptyString,
    alcoholConcentration: NonNegDouble,
    driverHealth: HealthType,
    doctorSignature: AssetId,
  )
