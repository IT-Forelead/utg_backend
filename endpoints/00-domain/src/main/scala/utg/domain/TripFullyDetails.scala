package utg.domain

import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class TripFullyDetails(
    trip: Trip,
    tripDrivers: List[TripDriver],
    tripFuelSupplies: List[TripFuelSupply],
    tripVehicleAcceptances: List[TripVehicleAcceptance],
    tripVehicleIndicators: List[TripVehicleIndicator],
    tripGivenFuels: List[TripGivenFuel],
    tripFuelInspections: List[TripFuelInspection],
    tripFuelRates: List[TripFuelRate],
    tripDriverTasks: List[TripDriverTask],
    tripCompleteTasks: List[TripCompleteTask],
    tripCompleteTaskAcceptances: List[TripCompleteTaskAcceptance],
    tripRouteDelays: List[TripRouteDelay],
  )
