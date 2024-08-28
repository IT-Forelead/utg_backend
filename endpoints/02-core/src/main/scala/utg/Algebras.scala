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
    medicalExaminationsAlgebra: MedicalExaminationsAlgebra[F],
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
    val assetsAlgebra = AssetsAlgebra.make[F](repositories.assets, s3Client)
    Algebras[F](
      auth = auth,
      assets = assetsAlgebra,
      users = UsersAlgebra.make[F](repositories.users, assetsAlgebra, opersms),
      roles = RolesAlgebra.make[F](repositories.roles),
      regions = RegionsAlgebra.make[F](repositories.regions),
      branches = BranchesAlgebra.make[F](repositories.branches, repositories.regions),
      vehicleCategories = VehicleCategoriesAlgebra.make[F](repositories.vehicleCategories),
      vehicles = VehiclesAlgebra.make[F](repositories.vehicles),
      trips = TripsAlgebra.make[F](
        repositories.trips,
        repositories.tripDrivers,
        repositories.tripTrailers,
        repositories.tripSemiTrailers,
        repositories.tripAccompanyingPersons,
        repositories.users,
        repositories.vehicles,
      ),
      tripVehicleIndicators = TripVehicleIndicatorsAlgebra.make[F](
        repositories.tripVehicleIndicators,
        repositories.trips,
      ),
      tripGivenFuelsAlgebra = TripGivenFuelsAlgebra.make[F](
        repositories.tripGivenFuels,
        repositories.trips,
        repositories.users,
      ),
      tripFuelInspectionsAlgebra = TripFuelInspectionsAlgebra.make[F](
        repositories.tripFuelInspections,
        repositories.trips,
        repositories.users,
      ),
      tripFuelRatesAlgebra = TripFuelRatesAlgebra.make[F](
        repositories.tripFuelRates,
        repositories.trips,
        repositories.users,
      ),
      tripVehicleAcceptancesAlgebra = TripVehicleAcceptancesAlgebra.make[F](
        repositories.tripVehicleAcceptances,
        repositories.users,
        repositories.trips,
      ),
      tripDriverTasks = TripDriverTasksAlgebra.make[F](
        repositories.tripDriverTasks,
        repositories.trips,
      ),
      tripRouteDelaysAlgebra = TripRouteDelaysAlgebra.make[F](
        repositories.tripRouteDelays,
        repositories.trips,
        repositories.users,
      ),
      tripCompleteTasksAlgebra = TripCompleteTasksAlgebra.make[F](
        repositories.tripCompleteTasks,
        repositories.trips,
        repositories.users,
      ),
      tripCompleteTaskAcceptancesAlgebra = TripCompleteTaskAcceptancesAlgebra.make[F](
        repositories.tripCompleteTaskAcceptances,
        repositories.trips,
        repositories.users,
      ),
      medicalExaminationsAlgebra = MedicalExaminationsAlgebra.make[F](
        repositories.medicalExaminations,
        repositories.trips,
        repositories.tripDrivers,
        repositories.users,
      ),
      vehicleHistoriesAlgebra = VehicleHistoriesAlgebra.make[F](repositories.vehicleHistories),
    )
  }
}
