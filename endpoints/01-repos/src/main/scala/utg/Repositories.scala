package utg

import cats.effect.Async
import cats.effect.Resource
import skunk.Session

import utg.repos._

case class Repositories[F[_]](
    users: UsersRepository[F],
    assets: AssetsRepository[F],
    roles: RolesRepository[F],
    regions: RegionsRepository[F],
    branches: BranchesRepository[F],
    vehicleCategories: VehicleCategoriesRepository[F],
    vehicles: VehiclesRepository[F],
    trips: TripsRepository[F],
    tripDrivers: TripDriversRepository[F],
    tripTrailers: TripTrailersRepository[F],
    tripSemiTrailers: TripSemiTrailersRepository[F],
    tripAccompanyingPersons: TripAccompanyingPersonsRepository[F],
    tripVehicleIndicators: TripVehicleIndicatorsRepository[F],
    tripGivenFuels: TripGivenFuelsRepository[F],
    tripFuelInspections: TripFuelInspectionsRepository[F],
    tripFuelInspectionItems: TripFuelInspectionItemsRepository[F],
    tripFuelRates: TripFuelRatesRepository[F],
    tripVehicleAcceptances: TripVehicleAcceptancesRepository[F],
    tripDriverTasks: TripDriverTasksRepository[F],
    tripRouteDelays: TripRouteDelaysRepository[F],
    tripCompleteTasks: TripCompleteTasksRepository[F],
    tripCompleteTaskAcceptances: TripCompleteTaskAcceptancesRepository[F],
    medicalExaminations: MedicalExaminationsRepository[F],
    vehicleHistories: VehicleHistoriesRepository[F],
  )

object Repositories {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): Repositories[F] = {
    val usersRepo = UsersRepository.make[F]
    Repositories(
      users = usersRepo,
      assets = AssetsRepository.make[F],
      roles = RolesRepository.make[F],
      regions = RegionsRepository.make[F],
      branches = BranchesRepository.make[F],
      vehicleCategories = VehicleCategoriesRepository.make[F],
      vehicles = VehiclesRepository.make[F],
      trips = TripsRepository.make[F],
      tripDrivers = TripDriversRepository.make[F](usersRepo),
      tripTrailers = TripTrailersRepository.make[F],
      tripSemiTrailers = TripSemiTrailersRepository.make[F],
      tripAccompanyingPersons = TripAccompanyingPersonsRepository.make[F],
      tripVehicleIndicators = TripVehicleIndicatorsRepository.make[F],
      tripGivenFuels = TripGivenFuelsRepository.make[F],
      tripFuelInspections = TripFuelInspectionsRepository.make[F],
      tripFuelInspectionItems = TripFuelInspectionItemsRepository.make[F],
      tripFuelRates = TripFuelRatesRepository.make[F],
      tripVehicleAcceptances = TripVehicleAcceptancesRepository.make[F],
      tripDriverTasks = TripDriverTasksRepository.make[F],
      tripRouteDelays = TripRouteDelaysRepository.make[F],
      tripCompleteTasks = TripCompleteTasksRepository.make[F],
      tripCompleteTaskAcceptances = TripCompleteTaskAcceptancesRepository.make[F],
      medicalExaminations = MedicalExaminationsRepository.make[F],
      vehicleHistories = VehicleHistoriesRepository.make[F],
    )
  }
}
