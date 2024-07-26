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
    tripFuelExpensesAlgebra: TripFuelExpensesAlgebra[F],
    tripVehicleAcceptancesAlgebra: TripVehicleAcceptancesAlgebra[F],
    tripDriverTasks: TripDriverTasksAlgebra[F],
    lineDelays: LineDelaysAlgebra[F],
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
      tripVehicleIndicators,
      tripFuelExpenses,
      tripVehicleAcceptances,
      tripDriverTasks,
      lineDelays,
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
      vehicles = VehiclesAlgebra.make[F](vehicles, branches, vehicleCategories),
      trips = TripsAlgebra.make[F](trips, users, vehicles),
      tripVehicleIndicators = TripVehicleIndicatorsAlgebra.make[F](tripVehicleIndicators, trips),
      tripFuelExpensesAlgebra = TripFuelExpensesAlgebra.make[F](tripFuelExpenses, users, trips),
      tripVehicleAcceptancesAlgebra = TripVehicleAcceptancesAlgebra.make[F](
        tripVehicleAcceptances,
        users,
        trips,
      ),
      tripDriverTasks = TripDriverTasksAlgebra.make[F](tripDriverTasks, trips),
      lineDelays = LineDelaysAlgebra.make[F](lineDelays, trips),
    )
  }
}
