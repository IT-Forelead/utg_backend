package utg.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.std.Random
import cats.implicits._
import org.typelevel.log4cats.Logger

import utg.domain.TripFullyDetails
import utg.domain.TripId
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError

trait TripFullyDetailsAlgebra[F[_]] {
  def getFullyDetails(id: TripId): F[TripFullyDetails]
}

object TripFullyDetailsAlgebra {
  def make[F[_]: Calendar: GenUUID: Random](
      tripsAlgebra: TripsAlgebra[F],
      tripDriversAlgebra: TripDriversAlgebra[F],
      tripFuelSuppliesAlgebra: TripFuelSuppliesAlgebra[F],
      tripVehicleAcceptancesAlgebra: TripVehicleAcceptancesAlgebra[F],
      tripVehicleIndicatorsAlgebra: TripVehicleIndicatorsAlgebra[F],
      tripGivenFuelsAlgebra: TripGivenFuelsAlgebra[F],
      tripFuelInspectionsAlgebra: TripFuelInspectionsAlgebra[F],
      tripFuelRatesAlgebra: TripFuelRatesAlgebra[F],
      tripDriverTasksAlgebra: TripDriverTasksAlgebra[F],
      tripCompleteTasksAlgebra: TripCompleteTasksAlgebra[F],
      tripCompleteTaskAcceptancesAlgebra: TripCompleteTaskAcceptancesAlgebra[F],
      tripRouteDelaysAlgebra: TripRouteDelaysAlgebra[F],
    )(implicit
      F: MonadThrow[F],
      logger: Logger[F],
    ): TripFullyDetailsAlgebra[F] =
    new TripFullyDetailsAlgebra[F] {
      override def getFullyDetails(id: TripId): F[TripFullyDetails] =
        OptionT(tripsAlgebra.findById(id)).cataF(
          AError
            .Internal(s"Trip not found by id [$id]")
            .raiseError[F, TripFullyDetails],
          trip =>
            for {
              tripDrivers <- tripDriversAlgebra.getByTripId(id)
              tripFuelSupplies <- tripFuelSuppliesAlgebra.getByTripId(id)
              tripVehicleAcceptances <- tripVehicleAcceptancesAlgebra.getByTripId(id)
              tripVehicleIndicators <- tripVehicleIndicatorsAlgebra.getByTripId(id)
              tripGivenFuels <- tripGivenFuelsAlgebra.getByTripId(id)
              tripFuelInspections <- tripFuelInspectionsAlgebra.getByTripId(id)
              tripFuelRates <- tripFuelRatesAlgebra.getByTripId(id)
              tripDriverTasks <- tripDriverTasksAlgebra.getByTripId(id)
              tripCompleteTasks <- tripCompleteTasksAlgebra.getByTripId(id)
              tripCompleteTaskAcceptances <- tripCompleteTaskAcceptancesAlgebra.getByTripId(id)
              tripRouteDelays <- tripRouteDelaysAlgebra.getByTripId(id)
              tripFullyDetails = TripFullyDetails(
                trip = trip,
                tripDrivers = tripDrivers,
                tripFuelSupplies = tripFuelSupplies,
                tripVehicleAcceptances = tripVehicleAcceptances,
                tripVehicleIndicators = tripVehicleIndicators,
                tripGivenFuels = tripGivenFuels,
                tripFuelInspections = tripFuelInspections,
                tripFuelRates = tripFuelRates,
                tripDriverTasks = tripDriverTasks,
                tripCompleteTasks = tripCompleteTasks,
                tripCompleteTaskAcceptances = tripCompleteTaskAcceptances,
                tripRouteDelays = tripRouteDelays,
              )
            } yield tripFullyDetails,
        )
    }
}
