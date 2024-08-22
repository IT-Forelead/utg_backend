package utg

import cats.MonadThrow
import cats.effect.std.Random
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt
import uz.scala.aws.s3.S3Client
import uz.scala.integration.sms.OperSmsClient

import utg.algebras._
import utg.auth.impl.Auth
import utg.domain.AuthedUser
import utg.effects.Calendar
import utg.effects.GenUUID

case class Algebras[F[_]](
    auth: Auth[F, AuthedUser],
    assets: AssetsAlgebra[F],
    users: UsersAlgebra[F],
    roles: RolesAlgebra[F],
    regions: RegionsAlgebra[F],
    branches: BranchesAlgebra[F],
    vehicleCategories: VehicleCategoriesAlgebra[F],
    vehicles: VehiclesAlgebra[F],
    trips: TripsAlgebra[F],
    tripVehicleIndicators: TripVehicleIndicatorsAlgebra[F],
    tripGivenFuelsAlgebra: TripGivenFuelsAlgebra[F],
    tripFuelInspectionsAlgebra: TripFuelInspectionsAlgebra[F],
    tripFuelRatesAlgebra: TripFuelRatesAlgebra[F],
    tripVehicleAcceptancesAlgebra: TripVehicleAcceptancesAlgebra[F],
    tripDriverTasks: TripDriverTasksAlgebra[F],
    tripRouteDelaysAlgebra: TripRouteDelaysAlgebra[F],
    tripCompleteTasksAlgebra: TripCompleteTasksAlgebra[F],
    tripCompleteTaskAcceptancesAlgebra: TripCompleteTaskAcceptancesAlgebra[F],
    completeTasksAlgebra: CompleteTasksAlgebra[F],
    vehicleHistoriesAlgebra: VehicleHistoriesAlgebra[F],
  )

object Algebras {
  def make[F[_]: MonadThrow: Calendar: GenUUID: Logger: Random: Lambda[M[_] => fs2.Compiler[M, M]]](
      auth: Auth[F, AuthedUser],
      repositories: Repositories[F],
      s3Client: S3Client[F],
      opersms: OperSmsClient[F],
    )(implicit
      P: PasswordHasher[F, SCrypt]
    ): Algebras[F] = {
    val Repositories(
      users,
      assets,
      roles,
      regions,
      branches,
      vehicleCategories,
      vehicles,
      trips,
      tripDrivers,
      tripAccompanyingPersons,
      tripVehicleIndicators,
      tripGivenFuels,
      tripFuelInspections,
      tripFuelRates,
      tripVehicleAcceptances,
      tripDriverTasks,
      tripRouteDelays,
      tripCompleteTasks,
      tripCompleteTaskAcceptances,
      completeTasks,
      vehicleHistories,
    ) = repositories
    val assetsAlgebra = AssetsAlgebra.make[F](assets, s3Client)
    Algebras[F](
      auth = auth,
      assets = assetsAlgebra,
      users = UsersAlgebra.make[F](users, assetsAlgebra, opersms),
      roles = RolesAlgebra.make[F](roles),
      regions = RegionsAlgebra.make[F](regions),
      branches = BranchesAlgebra.make[F](branches, regions),
      vehicleCategories = VehicleCategoriesAlgebra.make[F](vehicleCategories),
      vehicles = VehiclesAlgebra.make[F](vehicles),
      trips = TripsAlgebra.make[F](trips, tripDrivers, tripAccompanyingPersons, users, vehicles),
      tripVehicleIndicators = TripVehicleIndicatorsAlgebra.make[F](tripVehicleIndicators, trips),
      tripGivenFuelsAlgebra = TripGivenFuelsAlgebra.make[F](tripGivenFuels, trips, users),
      tripFuelInspectionsAlgebra =
        TripFuelInspectionsAlgebra.make[F](tripFuelInspections, trips, users),
      tripFuelRatesAlgebra = TripFuelRatesAlgebra.make[F](tripFuelRates, trips, users),
      tripVehicleAcceptancesAlgebra = TripVehicleAcceptancesAlgebra.make[F](
        tripVehicleAcceptances,
        users,
        trips,
      ),
      tripDriverTasks = TripDriverTasksAlgebra.make[F](tripDriverTasks, trips),
      tripRouteDelaysAlgebra = TripRouteDelaysAlgebra.make[F](tripRouteDelays, trips, users),
      tripCompleteTasksAlgebra = TripCompleteTasksAlgebra.make[F](tripCompleteTasks, trips, users),
      tripCompleteTaskAcceptancesAlgebra =
        TripCompleteTaskAcceptancesAlgebra.make[F](tripCompleteTaskAcceptances, trips, users),
      completeTasksAlgebra = CompleteTasksAlgebra.make[F](completeTasks, trips),
      vehicleHistoriesAlgebra = VehicleHistoriesAlgebra.make[F](vehicleHistories),
    )
  }
}
