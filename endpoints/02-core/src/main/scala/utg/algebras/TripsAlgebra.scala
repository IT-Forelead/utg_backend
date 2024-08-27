package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AuthedUser.User
import utg.domain._
import utg.domain.args.trips._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripAccompanyingPersonsRepository
import utg.repos.TripDriversRepository
import utg.repos.TripSemiTrailersRepository
import utg.repos.TripTrailersRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.VehiclesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripsAlgebra[F[_]] {
  def create(input: TripInput): F[TripId]
  def get(filters: TripFilters): F[ResponseData[Trip]]
  def findById(id: TripId): F[Option[Trip]]
  def update(input: UpdateTripInput): F[Unit]
  def updateDoctorApproval(input: TripDoctorApprovalInput): F[Unit]
  def updateChiefMechanicApproval(input: TripChiefMechanicInput): F[Unit]
}

object TripsAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripsRepository: TripsRepository[F],
      tripDriversRepository: TripDriversRepository[F],
      tripTrailersRepository: TripTrailersRepository[F],
      tripSemiTrailersRepository: TripSemiTrailersRepository[F],
      tripAccompanyingPersonsRepository: TripAccompanyingPersonsRepository[F],
      usersRepository: UsersRepository[F],
      vehicleRepository: VehiclesRepository[F],
    ): TripsAlgebra[F] =
    new TripsAlgebra[F] {
      private def makeAccompanyingPersons(
          tripId: TripId,
          userIds: NonEmptyList[UserId],
        ): F[NonEmptyList[dto.TripAccompanyingPerson]] =
        userIds.traverse { userId =>
          ID.make[F, TripAccompanyingPersonId].map { accId =>
            dto.TripAccompanyingPerson(
              id = accId,
              tripId = tripId,
              userId = userId,
            )
          }
        }

      private def createAccompanyingPersons(
          tripId: TripId,
          userIds: NonEmptyList[UserId],
        ): F[Unit] =
        for {
          list <- makeAccompanyingPersons(tripId, userIds)
          _ <- tripAccompanyingPersonsRepository.create(list)
        } yield ()

      private def updateAccompanyingPersons(
          tripId: TripId,
          userIds: NonEmptyList[UserId],
        ): F[Unit] =
        for {
          list <- makeAccompanyingPersons(tripId, userIds)
          _ <- tripAccompanyingPersonsRepository.deleteByTripId(tripId)
          _ <- tripAccompanyingPersonsRepository.create(list)
        } yield ()

      private def makeTrailers(
          tripId: TripId,
          trailerIds: NonEmptyList[VehicleId],
        ): F[NonEmptyList[dto.TripTrailer]] =
        trailerIds.traverse { trailerId =>
          ID.make[F, TripTrailerId].map { ttId =>
            dto.TripTrailer(
              id = ttId,
              tripId = tripId,
              trailerId = trailerId,
            )
          }
        }

      private def createTrailers(
          tripId: TripId,
          trailerIds: NonEmptyList[VehicleId],
        ): F[Unit] =
        for {
          list <- makeTrailers(tripId, trailerIds)
          _ <- tripTrailersRepository.create(list)
        } yield ()

      private def updateTrailers(
          tripId: TripId,
          trailerIds: NonEmptyList[VehicleId],
        ): F[Unit] =
        for {
          list <- makeTrailers(tripId, trailerIds)
          _ <- tripTrailersRepository.deleteByTripId(tripId)
          _ <- tripTrailersRepository.create(list)
        } yield ()

      private def makeSemiTrailers(
          tripId: TripId,
          semiTrailerIds: NonEmptyList[VehicleId],
        ): F[NonEmptyList[dto.TripSemiTrailer]] =
        semiTrailerIds.traverse { semiTrailerId =>
          ID.make[F, TripSemiTrailerId].map { tstId =>
            dto.TripSemiTrailer(
              id = tstId,
              tripId = tripId,
              semiTrailerId = semiTrailerId,
            )
          }
        }

      private def createSemiTrailers(
          tripId: TripId,
          semiTrailerIds: NonEmptyList[VehicleId],
        ): F[Unit] =
        for {
          list <- makeSemiTrailers(tripId, semiTrailerIds)
          _ <- tripSemiTrailersRepository.create(list)
        } yield ()

      private def updateSemiTrailers(
          tripId: TripId,
          semiTrailerIds: NonEmptyList[VehicleId],
        ): F[Unit] =
        for {
          list <- makeSemiTrailers(tripId, semiTrailerIds)
          _ <- tripSemiTrailersRepository.deleteByTripId(tripId)
          _ <- tripSemiTrailersRepository.create(list)
        } yield ()

      private def makeDrivers(
          driverId: UserId,
          tripId: TripId,
          driverIds: NonEmptyList[UserId],
        ): F[Option[dto.TripDriver]] =
        for {
          id <- ID.make[F, TripDriverId]
          driverByIds <- usersRepository.findByIds(driverIds)
          maybeDriverId = driverByIds.get(driverId).map(_.id)
          maybeDrivingLicenseNumber = driverByIds.get(driverId).flatMap(_.drivingLicenseNumber)
          tripDriver = (maybeDriverId, maybeDrivingLicenseNumber).mapN {
            case dId -> dln =>
              dto.TripDriver(
                id = id,
                tripId = tripId,
                driverId = dId,
                drivingLicenseNumber = dln,
              )
          }
        } yield tripDriver

      private def createDrivers(
          tripId: TripId,
          driverIds: NonEmptyList[UserId],
        ): F[Unit] =
        for {
          tripDrivers <- driverIds.traverse { driverId =>
            makeDrivers(driverId, tripId, driverIds)
          }
          _ <- NonEmptyList.fromList(tripDrivers.toList.flatten).traverse { drivers =>
            tripDriversRepository.create(drivers)
          }
        } yield ()

      private def updateDrivers(
          tripId: TripId,
          driverIds: NonEmptyList[UserId],
        ): F[Unit] =
        for {
          tripDrivers <- driverIds.traverse { driverId =>
            makeDrivers(driverId, tripId, driverIds)
          }
          _ <- tripDriversRepository.deleteByTripId(tripId)
          _ <- NonEmptyList.fromList(tripDrivers.toList.flatten).traverse { drivers =>
            tripDriversRepository.create(drivers)
          }
        } yield ()

      override def create(input: TripInput): F[TripId] =
        for {
          id <- ID.make[F, TripId]
          now <- Calendar[F].currentZonedDateTime
          dtoTrip = dto.Trip(
            id = id,
            createdAt = now,
            startDate = input.startDate,
            endDate = input.endDate,
            serialNumber = input.serialNumber,
            firstTab = input.firstTab,
            secondTab = input.secondTab,
            thirdTab = input.thirdTab,
            workingMode = input.workingMode,
            summation = input.summation,
            vehicleId = input.vehicleId,
            doctorId = None,
            doctorSignature = None,
            fuelSupply = None,
            chiefMechanicId = None,
            chiefMechanicSignature = None,
            notes = None,
          )
          _ <- tripsRepository.create(dtoTrip)
          _ <- createDrivers(id, input.driverIds)
          _ <- input.trailerIds.traverse { trailerId =>
            createTrailers(id, trailerId)
          }
          _ <- input.semiTrailerIds.traverse { semiTrailerId =>
            createSemiTrailers(id, semiTrailerId)
          }
          _ <- input.accompanyingPersonIds.traverse { userIds =>
            createAccompanyingPersons(id, userIds)
          }
        } yield id

      override def get(filters: TripFilters): F[ResponseData[Trip]] =
        for {
          dtoTrips <- tripsRepository.get(filters)
          accompanyingByTripId <- NonEmptyList
            .fromList(dtoTrips.data.map(_.id))
            .fold(Map.empty[TripId, List[dto.TripAccompanyingPerson]].pure[F]) { vehicleIds =>
              tripAccompanyingPersonsRepository.findByIds(vehicleIds)
            }
          trailersByTripId <- NonEmptyList
            .fromList(dtoTrips.data.map(_.id))
            .fold(Map.empty[TripId, List[dto.TripTrailer]].pure[F]) { vehicleIds =>
              tripTrailersRepository.findByIds(vehicleIds)
            }
          semiTrailersByTripId <- NonEmptyList
            .fromList(dtoTrips.data.map(_.id))
            .fold(Map.empty[TripId, List[dto.TripSemiTrailer]].pure[F]) { vehicleIds =>
              tripSemiTrailersRepository.findByIds(vehicleIds)
            }
          drivers <- NonEmptyList
            .fromList(dtoTrips.data.map(_.id))
            .fold(Map.empty[TripId, List[TripDriver]].pure[F]) { driverIds =>
              tripDriversRepository.findByTripIds(driverIds)
            }
          usersIds = dtoTrips
            .data
            .flatMap(t => t.doctorId ++ t.chiefMechanicId)
            .distinct
          accompanyingUsersIds = accompanyingByTripId.values.toList.flatMap(_.map(_.userId))
          vehicleIds = dtoTrips.data.map(_.vehicleId)
          trailersByTripIds = trailersByTripId.values.toList.flatMap(_.map(_.trailerId))
          semiTrailersByTripIds = semiTrailersByTripId.values.toList.flatMap(_.map(_.semiTrailerId))
          userById <- NonEmptyList
            .fromList(usersIds ++ accompanyingUsersIds)
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          vehicles <- NonEmptyList
            .fromList(vehicleIds ++ trailersByTripIds ++ semiTrailersByTripIds)
            .fold(Map.empty[VehicleId, Vehicle].pure[F]) { vehicleIds =>
              vehicleRepository.findByIds(vehicleIds.toList)
            }
          trip = dtoTrips.data.map { t =>
            val accompanyingUsers = accompanyingByTripId
              .get(t.id)
              .map(_.flatMap { ap =>
                userById.get(ap.userId)
              })
            val trailers = trailersByTripId
              .get(t.id)
              .map(_.flatMap { t =>
                vehicles.get(t.trailerId)
              })
            val semiTrailers = semiTrailersByTripId
              .get(t.id)
              .map(_.flatMap { st =>
                vehicles.get(st.semiTrailerId)
              })
            Trip(
              id = t.id,
              createdAt = t.createdAt,
              startDate = t.startDate,
              endDate = t.endDate,
              serialNumber = t.serialNumber,
              firstTab = t.firstTab,
              secondTab = t.secondTab,
              thirdTab = t.thirdTab,
              workingMode = t.workingMode,
              summation = t.summation,
              vehicle = vehicles.get(t.vehicleId),
              drivers = drivers.getOrElse(t.id, Nil),
              trailer = trailers,
              semiTrailer = semiTrailers,
              accompanyingPersons = accompanyingUsers,
              doctor = t.doctorId.flatMap(userById.get),
              doctorSignature = t.doctorSignature,
              fuelSupply = t.fuelSupply,
              chiefMechanic = t.chiefMechanicId.flatMap(userById.get),
              chiefMechanicSignature = t.chiefMechanicSignature,
              notes = t.notes,
            )
          }
        } yield dtoTrips.copy(data = trip)

      private def makeTrip(dtoTrip: dto.Trip): F[Trip] =
        for {
          accompanyingByTripId <- NonEmptyList
            .fromList(List(dtoTrip.id))
            .fold(Map.empty[TripId, List[dto.TripAccompanyingPerson]].pure[F]) { tIds =>
              tripAccompanyingPersonsRepository.findByIds(tIds)
            }
          trailerByTripId <- NonEmptyList
            .fromList(List(dtoTrip.id))
            .fold(Map.empty[TripId, List[dto.TripTrailer]].pure[F]) { tIds =>
              tripTrailersRepository.findByIds(tIds)
            }
          semiTrailerByTripId <- NonEmptyList
            .fromList(List(dtoTrip.id))
            .fold(Map.empty[TripId, List[dto.TripSemiTrailer]].pure[F]) { stIds =>
              tripSemiTrailersRepository.findByIds(stIds)
            }
          drivers <- tripDriversRepository.getByTripId(dtoTrip.id)
          usersIds = accompanyingByTripId.values.toList.flatMap(_.map(_.userId))
          trailersByTripIds = trailerByTripId.values.toList.flatMap(_.map(_.trailerId))
          semiTrailersByTripIds = semiTrailerByTripId.values.toList.flatMap(_.map(_.semiTrailerId))
          doctorWithMechanicIds = (dtoTrip.doctorId ++ dtoTrip.chiefMechanicId).toList.distinct
          userById <- NonEmptyList
            .fromList(usersIds ++ doctorWithMechanicIds)
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds)
            }
          vehicles <- vehicleRepository.findByIds(
            List(dtoTrip.vehicleId) ++ trailersByTripIds ++ semiTrailersByTripIds
          )
          accompanyingUsers = accompanyingByTripId
            .get(dtoTrip.id)
            .map(_.flatMap { ap =>
              userById.get(ap.userId)
            })
          trailers = trailerByTripId
            .get(dtoTrip.id)
            .map(_.flatMap { t =>
              vehicles.get(t.trailerId)
            })
          semiTrailers = semiTrailerByTripId
            .get(dtoTrip.id)
            .map(_.flatMap { st =>
              vehicles.get(st.semiTrailerId)
            })
          trip = Trip(
            id = dtoTrip.id,
            createdAt = dtoTrip.createdAt,
            startDate = dtoTrip.startDate,
            endDate = dtoTrip.endDate,
            serialNumber = dtoTrip.serialNumber,
            firstTab = dtoTrip.firstTab,
            secondTab = dtoTrip.secondTab,
            thirdTab = dtoTrip.thirdTab,
            workingMode = dtoTrip.workingMode,
            summation = dtoTrip.summation,
            vehicle = vehicles.get(dtoTrip.vehicleId),
            drivers = drivers,
            trailer = trailers,
            semiTrailer = semiTrailers,
            accompanyingPersons = accompanyingUsers,
            doctor = dtoTrip.doctorId.flatMap(userById.get),
            doctorSignature = dtoTrip.doctorSignature,
            fuelSupply = dtoTrip.fuelSupply,
            chiefMechanic = dtoTrip.chiefMechanicId.flatMap(userById.get),
            chiefMechanicSignature = dtoTrip.chiefMechanicSignature,
            notes = dtoTrip.notes,
          )
        } yield trip

      override def findById(id: TripId): F[Option[Trip]] =
        (for {
          dtoTrip <- OptionT(tripsRepository.findById(id))
          res <- OptionT.liftF(makeTrip(dtoTrip))
        } yield res).value

      override def update(input: UpdateTripInput): F[Unit] =
        for {
          _ <- tripsRepository.update(input.id) { dtoTrip =>
            dtoTrip.copy(
              startDate = input.startDate.getOrElse(dtoTrip.startDate),
              endDate = input.endDate,
              serialNumber = input.serialNumber,
              firstTab = input.firstTab,
              secondTab = input.secondTab,
              thirdTab = input.thirdTab,
              workingMode = input.workingMode.getOrElse(dtoTrip.workingMode),
              summation = input.summation,
              vehicleId = input.vehicleId.getOrElse(dtoTrip.vehicleId),
            )
          }
          _ <- input.driverIds.traverse { driverIds =>
            updateDrivers(input.id, driverIds)
          }
          _ <- input.trailerIds.traverse { trailerIds =>
            updateTrailers(input.id, trailerIds)
          }
          _ <- input.semiTrailerIds.traverse { semiTrailerIds =>
            updateSemiTrailers(input.id, semiTrailerIds)
          }
          _ <- input.accompanyingPersonIds.traverse { userIds =>
            updateAccompanyingPersons(input.id, userIds)
          }
        } yield {}

      override def updateDoctorApproval(input: TripDoctorApprovalInput): F[Unit] =
        OptionT(tripsRepository.findById(input.tripId))
          .cataF(
            AError
              .Internal(s"Trip not found by id [$input.tripId]")
              .raiseError[F, Unit],
            _ => tripsRepository.updateDoctorApproval(input),
          )

      override def updateChiefMechanicApproval(input: TripChiefMechanicInput): F[Unit] =
        OptionT(tripsRepository.findById(input.tripId))
          .cataF(
            AError
              .Internal(s"Trip not found by id [$input.tripId]")
              .raiseError[F, Unit],
            _ => tripsRepository.updateChiefMechanicApproval(input),
          )
    }
}
