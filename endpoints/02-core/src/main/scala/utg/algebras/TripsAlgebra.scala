package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AccompanyingPersonId
import utg.domain.ResponseData
import utg.domain.Trip
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.Vehicle
import utg.domain.VehicleId
import utg.domain.args.trips._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.VehiclesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripsAlgebra[F[_]] {
  def create(input: TripInput): F[TripId]
  def get(filters: TripFilters): F[ResponseData[Trip]]
  def findById(id: TripId): F[Option[Trip]]
  def updateDoctorApproval(input: TripDoctorApprovalInput): F[Unit]
  def updateChiefMechanicApproval(input: TripChiefMechanicInput): F[Unit]
  def updateDispatcher(input: TripDispatcherInput): F[Unit]
}

object TripsAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripsRepository: TripsRepository[F],
      usersRepository: UsersRepository[F],
      vehicleRepository: VehiclesRepository[F],
    ): TripsAlgebra[F] =
    new TripsAlgebra[F] {
      private def createAccompanyingPersons(
          tripId: TripId,
          userIds: NonEmptyList[UserId],
        ): F[Unit] =
        for {
          list <- userIds.traverse { userId =>
            ID.make[F, AccompanyingPersonId].map { accId =>
              dto.AccompanyingPerson(
                id = accId,
                tripId = tripId,
                userId = userId,
              )
            }
          }
          _ <- tripsRepository.createAccompanyingPersons(list.toList)
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
            firstTab = None,
            secondTab = None,
            thirdTab = None,
            workingMode = input.workingMode,
            summation = None,
            vehicleId = input.vehicleId,
            driverId = input.driverId,
            trailerId = None,
            semiTrailerId = None,
            doctorId = None,
            doctorSignature = None,
            fuelSupply = None,
            chiefMechanicId = None,
            chiefMechanicSignature = None,
            notes = None,
          )
          _ <- tripsRepository.create(dtoTrip)
        } yield id

      override def get(filters: TripFilters): F[ResponseData[Trip]] =
        for {
          dtoTrips <- tripsRepository.get(filters)
          accompanyingByTripId <- tripsRepository.findAccompanyingPersonByIds(
            dtoTrips.data.map(_.id)
          )
          usersIds = dtoTrips
            .data
            .flatMap(t => t.driverId.some ++ t.doctorId ++ t.chiefMechanicId)
            .distinct
          accompanyingUsersIds = accompanyingByTripId.values.toList.flatMap(_.map(_.userId))
          userById <- usersRepository.findByIds(usersIds ++ accompanyingUsersIds)
          vehicleIds = dtoTrips
            .data
            .flatMap(tva => tva.vehicleId.some ++ tva.trailerId ++ tva.semiTrailerId)
            .distinct
          vehicles <- NonEmptyList
            .fromList(vehicleIds)
            .fold(Map.empty[VehicleId, Vehicle].pure[F]) { vehicleIds =>
              vehicleRepository.findByIds(vehicleIds.toList)
            }
          trip = dtoTrips.data.map { t =>
            val accompanyingUsers = accompanyingByTripId
              .get(t.id)
              .map(_.flatMap { ap =>
                userById.get(ap.userId)
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
              driver = userById.get(t.driverId),
              trailer = t.trailerId.flatMap(vehicles.get),
              semiTrailer = t.semiTrailerId.flatMap(vehicles.get),
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
          accompanyingByTripId <- tripsRepository.findAccompanyingPersonByIds(List(dtoTrip.id))
          usersIds = accompanyingByTripId.values.toList.flatMap(_.map(_.userId))
          doctorWithMechanicIds = (dtoTrip
            .driverId
            .some ++ dtoTrip.doctorId ++ dtoTrip.chiefMechanicId).toList.distinct
          userById <- usersRepository.findByIds(usersIds ++ doctorWithMechanicIds)
          vehicleIds = (dtoTrip.vehicleId.some ++ dtoTrip.trailerId ++ dtoTrip.semiTrailerId)
            .toList
            .distinct
          vehicles <- vehicleRepository.findByIds(vehicleIds)
          accompanyingUsers = accompanyingByTripId
            .get(dtoTrip.id)
            .map(_.flatMap { ap =>
              userById.get(ap.userId)
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
            driver = userById.get(dtoTrip.driverId),
            trailer = dtoTrip.trailerId.flatMap(vehicles.get),
            semiTrailer = dtoTrip.semiTrailerId.flatMap(vehicles.get),
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

      override def updateDispatcher(input: TripDispatcherInput): F[Unit] =
        OptionT(tripsRepository.findById(input.tripId))
          .cataF(
            AError
              .Internal(s"Trip not found by id [$input.tripId]")
              .raiseError[F, Unit],
            _ => tripsRepository.update(input.tripId)(
              _.copy(
                firstTab = input.firstTab,
                secondTab = input.secondTab,
                thirdTab = input.thirdTab,
                workingMode = input.workingMode,
                summation = input.summation,
                vehicleId = input.vehicleId,
                driverId = input.driverId,
                trailerId = input.trailerId,
                semiTrailerId = input.semiTrailerId,
                doctorId = None,
                doctorSignature = None,
                fuelSupply = None,
                chiefMechanicId = None,
                chiefMechanicSignature = None,
              )
            )
          )
    }
}
