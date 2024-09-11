package utg

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.std.Random
import cats.implicits.toFunctorOps
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import uz.scala.http4s.HttpServer
import uz.scala.http4s.utils.Routes

import utg.domain.AuthedUser
import utg.http.Environment
import utg.routes._

object HttpModule {
  private def allRoutes[F[_]: Async: JsonDecoder: Dispatcher: Logger: Random](
      env: Environment[F]
    ): NonEmptyList[HttpRoutes[F]] =
    NonEmptyList
      .of[Routes[F, AuthedUser]](
        new AuthRoutes[F](env.algebras.auth),
        new UsersRoutes[F](env.algebras.users, env.algebras.roles),
        new VehicleCategoriesRoutes[F](env.algebras.vehicleCategories),
        new VehiclesRoutes[F](
          env.algebras.vehicles,
          env.algebras.branches,
          env.algebras.vehicleCategories,
        ),
        new RegionsRoutes[F](env.algebras.regions),
        new BranchesRoutes[F](env.algebras.branches),
        new TripsRoutes[F](env.algebras.trips, env.algebras.tripFullyDetails),
        new TripDriversRoutes[F](env.algebras.tripDrivers),
        new TripFuelSuppliesRoutes[F](env.algebras.tripFuelSupplies),
        new TripVehicleIndicatorsRoutes[F](env.algebras.tripVehicleIndicators),
        new TripGivenFuelsRoutes[F](env.algebras.tripGivenFuels),
        new TripFuelInspectionsRoutes[F](env.algebras.tripFuelInspections),
        new TripFuelRatesRoutes[F](env.algebras.tripFuelRates),
        new AssetsRoutes[F](env.algebras.assets),
        new TripVehicleAcceptancesRoutes[F](env.algebras.tripVehicleAcceptances),
        new TripDriverTasksRoutes[F](env.algebras.tripDriverTasks),
        new TripRouteDelaysRoutes[F](env.algebras.tripRouteDelays),
        new TripCompleteTasksRoutes[F](env.algebras.tripCompleteTasks),
        new TripCompleteTaskAcceptancesRoutes[F](env.algebras.tripCompleteTaskAcceptances),
        new MedicalExaminationsRoutes[F](env.algebras.medicalExaminations),
        new SmsMessagesRoutes[F](env.algebras.smsMessages),
      )
      .map { r =>
        Router(
          r.path -> (r.public <+> env.middleware(r.`private`))
        )
      }

  def make[F[_]: Async: Dispatcher: Random](
      env: Environment[F]
    )(implicit
      logger: Logger[F]
    ): Resource[F, F[ExitCode]] =
    HttpServer.make[F](env.config, _ => allRoutes[F](env)).map { _ =>
      logger.info(s"HTTP server is started").as(ExitCode.Success)
    }
}
