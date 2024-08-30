package utg.domain.args.tripDrivers

import io.circe.generic.JsonCodec

import utg.domain.AssetId
import utg.domain.MedicalExaminationId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.enums.HealthType

@JsonCodec
case class UpdateDriverExamination(
    tripId: TripId,
    driverId: UserId,
    driverHealth: Option[HealthType],
    doctorId: Option[UserId],
    doctorSignature: Option[AssetId],
    medicalExaminationId: Option[MedicalExaminationId],
  )
