package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.implicits._

import utg.domain.AccompanyingPersonId
import utg.domain.ResponseData
import utg.domain.Trip
import utg.domain.TripId
import utg.domain.UserId
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
          )
          _ <- tripsRepository.create(dtoTrip)
          _ <- input.accompanyingPersons.traverse { userIds =>
            createAccompanyingPersons(id, userIds)
          }
        } yield id

      override def get(filters: TripFilters): F[ResponseData[Trip]] =
        for {
          dtoTrips <- tripsRepository.get(filters)
          vehicles <- vehicleRepository.findByIds(dtoTrips.data.map(_.vehicleId))
          drivers <- usersRepository.findByIds(dtoTrips.data.map(_.driverId))
          trailers <- vehicleRepository.findByIds(dtoTrips.data.flatMap(_.trailerId))
          semiTrailers <- vehicleRepository.findByIds(dtoTrips.data.flatMap(_.semiTrailerId))
          trip = dtoTrips.data.map { t =>
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
              semiTrailer = t.semiTrailerId.flatMap(semiTrailers.get),
            )
          }
        } yield dtoTrips.copy(data = trip)
    }
}
