package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._

import utg.domain.AccompanyingPersonId
import utg.domain.AuthedUser.User
import utg.domain.ResponseData
import utg.domain.Trip
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.Vehicle
import utg.domain.VehicleId
import utg.domain.args.trips.TripFilters
import utg.domain.args.trips.TripInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.VehiclesRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripsAlgebra[F[_]] {
  def create(input: TripInput): F[TripId]
  def get(filters: TripFilters): F[ResponseData[Trip]]
  def findById(id: TripId): F[Option[Trip]]
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
            chiefMechanicId = None,
            doctorSignature = None,
            chiefMechanicSignature = None,
            notes = None,
          )
          _ <- tripsRepository.create(dtoTrip)
          _ <- input.accompanyingPersons.traverse { userIds =>
            createAccompanyingPersons(id, userIds)
          }
        } yield id

      override def get(filters: TripFilters): F[ResponseData[Trip]] =
        for {
          dtoTrips <- tripsRepository.get(filters)
          accompanyingByTripId <- tripsRepository.findAccompanyingPersonByIds(
            dtoTrips.data.map(_.id)
          )
          doctorWithMechanicIds =  dtoTrips.data.flatMap { t => t.doctorId ++ t.chiefMechanicId }
          usersIds = accompanyingByTripId.values.toList.flatMap(_.map(_.userId))
          userById <- NonEmptyList.fromList(usersIds).fold(Map.empty[UserId, User].pure[F]) {
            userIds =>
              usersRepository.findByIds(userIds.toList ++ doctorWithMechanicIds)
          }
          vehicles <- NonEmptyList
            .fromList(dtoTrips.data.map(_.vehicleId))
            .fold(Map.empty[VehicleId, Vehicle].pure[F]) { vehicleIds =>
              vehicleRepository.findByIds(vehicleIds.toList)
            }
          trailerIds = dtoTrips.data.flatMap(tva => tva.trailerId ++ tva.semiTrailerId).distinct
          drivers <- NonEmptyList
            .fromList(dtoTrips.data.map(_.driverId))
            .fold(Map.empty[UserId, User].pure[F]) { userIds =>
              usersRepository.findByIds(userIds.toList)
            }
          trailers <- NonEmptyList
            .fromList(trailerIds)
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
              driver = drivers.get(t.driverId),
              trailer = t.trailerId.flatMap(trailers.get),
              semiTrailer = t.semiTrailerId.flatMap(trailers.get),
              accompanyingPersons = accompanyingUsers,
              doctor = doctors.get(t.doctorId),
              doctorSignature = t.doctorSignature,
              chiefMechanic = chiefMechanics.get(t.chiefMechanicId),
              chiefMechanicSignature = t.chiefMechanicSignature,
              notes = t.notes,
            )
          }
        } yield dtoTrips.copy(data = trip)

      private def makeTrip(dtoTrip: dto.Trip): F[Trip] =
        for {
          accompanyingByTripId <- tripsRepository.findAccompanyingPersonByIds(List(dtoTrip.id))
          usersIds = accompanyingByTripId.values.toList.flatMap(_.map(_.userId))
          userById <- usersRepository.findByIds(usersIds)
          vehicles <- vehicleRepository.findByIds(List(dtoTrip.vehicleId))
          drivers <- usersRepository.findByIds(List(dtoTrip.driverId))
          trailers <- vehicleRepository.findByIds(dtoTrip.trailerId.toList)
          semiTrailers <- vehicleRepository.findByIds(dtoTrip.semiTrailerId.toList)
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
            driver = drivers.get(dtoTrip.driverId),
            trailer = dtoTrip.trailerId.flatMap(trailers.get),
            semiTrailer = dtoTrip.semiTrailerId.flatMap(semiTrailers.get),
            accompanyingPersons = accompanyingUsers,
            doctor = doctors.get(dtoTrip.doctorId),
            doctorSignature = dtoTrip.doctorSignature,
            chiefMechanic = chiefMechanics.get(dtoTrip.chiefMechanicId),
            chiefMechanicSignature = dtoTrip.chiefMechanicSignature,
            notes = dtoTrip.notes,
          )
        } yield trip

      override def findById(id: TripId): F[Option[Trip]] =
        (for {
          dtoTrip <- OptionT(tripsRepository.findById(id))
          res <- OptionT.liftF(makeTrip(dtoTrip))
        } yield res).value
    }
}
