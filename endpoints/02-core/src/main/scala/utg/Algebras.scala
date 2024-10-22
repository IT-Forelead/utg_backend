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
    tripFullyDetails: TripFullyDetailsAlgebra[F],
    tripDrivers: TripDriversAlgebra[F],
    tripFuelSupplies: TripFuelSuppliesAlgebra[F],
    tripVehicleIndicators: TripVehicleIndicatorsAlgebra[F],
    tripGivenFuels: TripGivenFuelsAlgebra[F],
    tripFuelInspections: TripFuelInspectionsAlgebra[F],
    tripFuelRates: TripFuelRatesAlgebra[F],
    tripVehicleAcceptances: TripVehicleAcceptancesAlgebra[F],
    tripDriverTasks: TripDriverTasksAlgebra[F],
    tripRouteDelays: TripRouteDelaysAlgebra[F],
    tripCompleteTasks: TripCompleteTasksAlgebra[F],
    tripCompleteTaskAcceptances: TripCompleteTaskAcceptancesAlgebra[F],
    medicalExaminations: MedicalExaminationsAlgebra[F],
    vehicleHistories: VehicleHistoriesAlgebra[F],
    smsMessages: SmsMessagesAlgebra[F],
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
    val tripsAlgebra = TripsAlgebra.make[F](
      repositories.trips,
      repositories.tripDrivers,
      repositories.tripTrailers,
      repositories.tripSemiTrailers,
      repositories.tripAccompanyingPersons,
      repositories.users,
      repositories.vehicles,
    )
    val tripFuelSuppliesAlgebra = TripFuelSuppliesAlgebra.make[F](
      repositories.tripFuelSupplies,
      repositories.tripFuelSupplyItems,
      repositories.trips,
      repositories.users,
    )
    val tripVehicleIndicatorsAlgebra = TripVehicleIndicatorsAlgebra.make[F](
      repositories.tripVehicleIndicators,
      repositories.trips,
    )
    val tripGivenFuelsAlgebra = TripGivenFuelsAlgebra.make[F](
      repositories.tripGivenFuels,
      repositories.trips,
      repositories.users,
      assetsAlgebra,
    )
    val tripFuelInspectionsAlgebra = TripFuelInspectionsAlgebra.make[F](
      repositories.tripFuelInspections,
      repositories.tripFuelInspectionItems,
      repositories.trips,
      repositories.users,
    )
    val tripFuelRatesAlgebra = TripFuelRatesAlgebra.make[F](
      repositories.tripFuelRates,
      repositories.trips,
      repositories.users,
    )
    val tripVehicleAcceptancesAlgebra = TripVehicleAcceptancesAlgebra.make[F](
      repositories.tripVehicleAcceptances,
      repositories.users,
      repositories.trips,
    )
    val tripDriverTasksAlgebra = TripDriverTasksAlgebra.make[F](
      repositories.tripDriverTasks,
      repositories.trips,
    )
    val tripRouteDelaysAlgebra = TripRouteDelaysAlgebra.make[F](
      repositories.tripRouteDelays,
      repositories.trips,
      repositories.users,
    )
    val tripCompleteTasksAlgebra = TripCompleteTasksAlgebra.make[F](
      repositories.tripCompleteTasks,
      repositories.trips,
      repositories.users,
    )
    val tripCompleteTaskAcceptancesAlgebra = TripCompleteTaskAcceptancesAlgebra.make[F](
      repositories.tripCompleteTaskAcceptances,
      repositories.trips,
      repositories.users,
    )
    val tripDriversAlgebra = TripDriversAlgebra.make[F](repositories.tripDrivers)
    val smsMessagesAlgebra = SmsMessagesAlgebra.make[F](repositories.smsMessages, opersms)
    Algebras[F](
      auth = auth,
      assets = assetsAlgebra,
      users = UsersAlgebra.make[F](
        repositories.users,
        repositories.userLicensePhotos,
        smsMessagesAlgebra
      ),
      roles = RolesAlgebra.make[F](repositories.roles),
      regions = RegionsAlgebra.make[F](repositories.regions),
      branches = BranchesAlgebra.make[F](repositories.branches, repositories.regions),
      vehicleCategories = VehicleCategoriesAlgebra.make[F](repositories.vehicleCategories),
      vehicles = VehiclesAlgebra.make[F](
        repositories.vehicles,
        repositories.vehicleFuelItems,
        repositories.vehiclePhotos,
        repositories.vehicleLicensePhotos,
        assetsAlgebra
      ),
      trips = tripsAlgebra,
      tripDrivers = tripDriversAlgebra,
      tripFuelSupplies = tripFuelSuppliesAlgebra,
      tripVehicleIndicators = tripVehicleIndicatorsAlgebra,
      tripGivenFuels = tripGivenFuelsAlgebra,
      tripFuelInspections = tripFuelInspectionsAlgebra,
      tripFuelRates = tripFuelRatesAlgebra,
      tripVehicleAcceptances = tripVehicleAcceptancesAlgebra,
      tripDriverTasks = tripDriverTasksAlgebra,
      tripRouteDelays = tripRouteDelaysAlgebra,
      tripCompleteTasks = tripCompleteTasksAlgebra,
      tripCompleteTaskAcceptances = tripCompleteTaskAcceptancesAlgebra,
      medicalExaminations = MedicalExaminationsAlgebra.make[F](
        repositories.medicalExaminations,
        repositories.trips,
        repositories.tripDrivers,
        repositories.users,
      ),
      vehicleHistories = VehicleHistoriesAlgebra.make[F](repositories.vehicleHistories),
      tripFullyDetails = TripFullyDetailsAlgebra.make[F](
        tripsAlgebra,
        tripDriversAlgebra,
        tripFuelSuppliesAlgebra,
        tripVehicleAcceptancesAlgebra,
        tripVehicleIndicatorsAlgebra,
        tripGivenFuelsAlgebra,
        tripFuelInspectionsAlgebra,
        tripFuelRatesAlgebra,
        tripDriverTasksAlgebra,
        tripCompleteTasksAlgebra,
        tripCompleteTaskAcceptancesAlgebra,
        tripRouteDelaysAlgebra,
      ),
      smsMessages = smsMessagesAlgebra,
    )
  }
}
