package utg.repos

import cats.effect.Async
import cats.effect.Resource
import eu.timepit.refined.types.numeric.NonNegDouble
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.domain.VehicleId
import utg.repos.sql.TripVehicleIndicatorsSql
import utg.repos.sql.dto

trait TripVehicleIndicatorsRepository[F[_]] {
  def create(input: dto.TripVehicleIndicator): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripVehicleIndicator]]
  def getLastOdometerIndicatorByVehicleId(vehicleId: VehicleId): F[Option[NonNegDouble]]
}

object TripVehicleIndicatorsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripVehicleIndicatorsRepository[F] = new TripVehicleIndicatorsRepository[F] {
    override def create(input: dto.TripVehicleIndicator): F[Unit] =
      TripVehicleIndicatorsSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripVehicleIndicator]] =
      TripVehicleIndicatorsSql.selectByTripId.queryList(tripId)

    override def getLastOdometerIndicatorByVehicleId(
        vehicleId: VehicleId
      ): F[Option[NonNegDouble]] =
      TripVehicleIndicatorsSql.selectLastOdometerIndicatorByVehicleId.queryOption(vehicleId)
  }
}
