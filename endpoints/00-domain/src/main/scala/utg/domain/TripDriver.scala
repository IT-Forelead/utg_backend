package utg.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User
import utg.domain.enums.HealthType

@JsonCodec
case class TripDriver(
    id: TripDriverId,
    tripId: TripId,
    driver: Option[User],
    drivingLicenseNumber: NonEmptyString,
    driverHealth: Option[HealthType] = None,
    doctor: Option[User] = None,
    doctorSignature: Option[AssetId] = None,
    medicalExaminationId: Option[MedicalExaminationId] = None,
  )
