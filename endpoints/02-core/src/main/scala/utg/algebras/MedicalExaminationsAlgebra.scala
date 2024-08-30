package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AuthedUser.User
import utg.domain.MedicalExamination
import utg.domain.MedicalExaminationId
import utg.domain.ResponseData
import utg.domain.UserId
import utg.domain.args.medicalExaminations._
import utg.domain.args.tripDrivers.UpdateDriverExamination
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.MedicalExaminationsRepository
import utg.repos.TripDriversRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait MedicalExaminationsAlgebra[F[_]] {
  def create(input: MedicalExaminationInput, doctorId: UserId): F[MedicalExaminationId]
  def get(filters: MedicalExaminationFilters): F[ResponseData[MedicalExamination]]
}

object MedicalExaminationsAlgebra {
  def make[F[_]: Calendar: GenUUID](
      medicalExaminationsRepository: MedicalExaminationsRepository[F],
      tripsRepository: TripsRepository[F],
      tripDriversRepository: TripDriversRepository[F],
      usersRepository: UsersRepository[F],
    )(implicit
      F: MonadThrow[F]
    ): MedicalExaminationsAlgebra[F] =
    new MedicalExaminationsAlgebra[F] {
      override def create(
          input: MedicalExaminationInput,
          doctorId: UserId,
        ): F[MedicalExaminationId] =
        for {
          trip <- OptionT(tripsRepository.findById(input.tripId))
            .getOrRaise(AError.Internal(s"Trip not found by id [${input.tripId}]"))
          tripDriver <- OptionT(
            tripDriversRepository
              .getByTripId(trip.id)
              .map(_.find(_.driver.exists(_.id == input.driverId)))
          ).getOrRaise(AError.Internal(s"Driver not found by id [${input.driverId}]"))
          id <- ID.make[F, MedicalExaminationId]
          now <- Calendar[F].currentZonedDateTime
          dtoData = dto.MedicalExamination(
            id = id,
            createdAt = now,
            tripId = trip.id,
            driverId = input.driverId,
            driverPersonalNumber = tripDriver.driver.get.personalNumber,
            complaint = input.complaint,
            pulse = input.pulse,
            bodyTemperature = input.bodyTemperature,
            bloodPressure = input.bloodPressure,
            alcoholConcentration = input.alcoholConcentration,
            driverHealth = input.driverHealth,
            doctorId = doctorId,
            doctorSignature = input.doctorSignature,
          )
          _ <- medicalExaminationsRepository.create(dtoData)
          _ <- tripDriversRepository.updateDriverExamination(
            UpdateDriverExamination(
              tripId = trip.id,
              driverId = input.driverId,
              driverHealth = input.driverHealth.some,
              doctorId = doctorId.some,
              doctorSignature = input.doctorSignature.some,
              medicalExaminationId = id.some,
            )
          )
        } yield id

      override def get(filters: MedicalExaminationFilters): F[ResponseData[MedicalExamination]] =
        for {
          dtoData <- medicalExaminationsRepository.get(filters)
          usersIds = dtoData
            .data
            .flatMap(med => List(med.driverId, med.doctorId))
          users <- NonEmptyList
            .fromList(usersIds)
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          medicalExamination = dtoData.data.map { me =>
            MedicalExamination(
              id = me.id,
              createdAt = me.createdAt,
              tripId = me.tripId,
              driver = users.get(me.driverId),
              driverPersonalNumber = me.driverPersonalNumber,
              complaint = me.complaint,
              pulse = me.pulse,
              bodyTemperature = me.bodyTemperature,
              bloodPressure = me.bloodPressure,
              alcoholConcentration = me.alcoholConcentration,
              driverHealth = me.driverHealth,
              doctor = users.get(me.doctorId),
              doctorSignature = me.doctorSignature,
            )
          }
        } yield dtoData.copy(data = medicalExamination)
    }
}
