package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString

import utg.domain.AuthedUser.User
import utg.domain.TripCompleteTaskAcceptance
import utg.domain.TripCompleteTaskAcceptanceId
import utg.domain.TripId
import utg.domain.UserId
import utg.domain.args.tripCompleteTasks.TripCompleteTaskAcceptanceInput
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.TripCompleteTaskAcceptancesRepository
import utg.repos.TripsRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait TripCompleteTaskAcceptancesAlgebra[F[_]] {
  def create(input: TripCompleteTaskAcceptanceInput): F[TripCompleteTaskAcceptanceId]
  def getByTripId(tripId: TripId): F[List[TripCompleteTaskAcceptance]]
}

object TripCompleteTaskAcceptancesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tripCompleteTaskAcceptancesRepository: TripCompleteTaskAcceptancesRepository[F],
      tripsRepository: TripsRepository[F],
      usersRepository: UsersRepository[F],
    ): TripCompleteTaskAcceptancesAlgebra[F] =
    new TripCompleteTaskAcceptancesAlgebra[F] {
      override def create(input: TripCompleteTaskAcceptanceInput): F[TripCompleteTaskAcceptanceId] =
        OptionT(tripsRepository.findById(input.tripId)).cataF(
          AError
            .Internal(s"Trip not found by id [$input.tripId]")
            .raiseError[F, TripCompleteTaskAcceptanceId],
          trip =>
            for {
              id <- ID.make[F, TripCompleteTaskAcceptanceId]
              now <- Calendar[F].currentZonedDateTime
              dtoTripCompleteTaskAcceptance = dto.TripCompleteTaskAcceptance(
                id = id,
                createdAt = now,
                tripId = trip.id,
                commuteNumberTotal = input
                  .commuteNumberTotal
                  .getOrElse(
                    NonNegInt.unsafeFrom(0)
                  ),
                loadNumberTotal = input
                  .loadNumberTotal
                  .getOrElse(
                    NonNegInt.unsafeFrom(0)
                  ),
                loadNumberTotalStr = input
                  .loadNumberTotalStr
                  .getOrElse(
                    NonEmptyString.unsafeFrom("nol")
                  ),
                documentId = input.documentId,
                driverId = input.driverId,
                driverSignature = input.driverSignature,
                dispatcherId = input.dispatcherId,
                dispatcherSignature = input.dispatcherSignature,
              )
              _ <- tripCompleteTaskAcceptancesRepository.create(dtoTripCompleteTaskAcceptance)
            } yield id,
        )

      override def getByTripId(tripId: TripId): F[List[TripCompleteTaskAcceptance]] =
        for {
          dtoTripCompleteTaskAcceptances <- tripCompleteTaskAcceptancesRepository.getByTripId(
            tripId
          )
          userIds = dtoTripCompleteTaskAcceptances
            .flatMap(tva => tva.driverId ++ tva.dispatcherId)
            .distinct
          users <- NonEmptyList.fromList(userIds).fold(Map.empty[UserId, User].pure[F]) {
            nonEmptyUserIds =>
              usersRepository.findByIds(nonEmptyUserIds)
          }
          tripCompleteTaskAcceptances = dtoTripCompleteTaskAcceptances.map(tcta =>
            TripCompleteTaskAcceptance(
              id = tcta.id,
              createdAt = tcta.createdAt,
              tripId = tcta.tripId,
              commuteNumberTotal = tcta.commuteNumberTotal,
              loadNumberTotal = tcta.loadNumberTotal,
              loadNumberTotalStr = tcta.loadNumberTotalStr,
              document = tcta.documentId,
              driver = tcta.driverId.flatMap(users.get),
              driverSignature = tcta.driverSignature,
              dispatcher = tcta.dispatcherId.flatMap(users.get),
              dispatcherSignature = tcta.dispatcherSignature,
            )
          )
        } yield tripCompleteTaskAcceptances
    }
}
