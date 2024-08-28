package utg.domain.args.medicalExaminations

import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.TripId
import utg.domain.UserId
import utg.domain.enums.HealthType

@JsonCodec
case class MedicalExaminationFilters(
    tripId: Option[TripId] = None,
    driverId: Option[UserId] = None,
    driverHealth: Option[HealthType] = None,
    limit: Option[NonNegInt] = None,
    page: Option[NonNegInt] = None,
  )
