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
    tripAccompanyingPersonsRepository: TripAccompanyingPersonsRepository[F],
    tripVehicleIndicators: TripVehicleIndicatorsRepository[F],
    tripGivenFuels: TripGivenFuelsRepository[F],
    tripFuelInspections: TripFuelInspectionsRepository[F],
    tripFuelRates: TripFuelRatesRepository[F],
    tripVehicleAcceptances: TripVehicleAcceptancesRepository[F],
    tripDriverTasks: TripDriverTasksRepository[F],
    lineDelays: LineDelaysRepository[F],
    completeTasks: CompleteTasksRepository[F],
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
      tripAccompanyingPersonsRepository = TripAccompanyingPersonsRepository.make[F],
      tripVehicleIndicators = TripVehicleIndicatorsRepository.make[F],
      tripGivenFuels = TripGivenFuelsRepository.make[F],
      tripFuelInspections = TripFuelInspectionsRepository.make[F],
      tripFuelRates = TripFuelRatesRepository.make[F],
      tripVehicleAcceptances = TripVehicleAcceptancesRepository.make[F],
      tripDriverTasks = TripDriverTasksRepository.make[F],
      lineDelays = LineDelaysRepository.make[F],
      completeTasks = CompleteTasksRepository.make[F],
      vehicleHistories = VehicleHistoriesRepository.make[F],
    )
  }
}
