package utg.repos

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.FuelTypeAndQuantity
import utg.domain.VehicleFuelItem
import utg.domain.VehicleFuelItemId
import utg.domain.VehicleId
import utg.effects.GenUUID
import utg.repos.sql.VehicleFuelItemsSql
import utg.repos.sql.dto
import utg.utils.ID

trait VehicleFuelItemsRepository[F[_]] {
  def create(
      vehicleId: VehicleId,
      fuels: NonEmptyList[FuelTypeAndQuantity],
    ): F[Unit]
  def getByVehicleId(id: VehicleId): F[List[dto.VehicleFuelItem]]
  def findByVehicleIds(ids: NonEmptyList[VehicleId]): F[Map[VehicleId, List[VehicleFuelItem]]]
}

object VehicleFuelItemsRepository {
  def make[F[_]: MonadThrow: Async: GenUUID](
      implicit
      session: Resource[F, Session[F]]
    ): VehicleFuelItemsRepository[F] = new VehicleFuelItemsRepository[F] {
    override def create(
        vehicleId: VehicleId,
        fuels: NonEmptyList[FuelTypeAndQuantity],
      ): F[Unit] =
      fuels.traverse_ { item =>
        for {
          id <- ID.make[F, VehicleFuelItemId]
          dtoData = dto.VehicleFuelItem(
            id = id,
            vehicleId = vehicleId,
            fuelType = item.fuelType,
            fuelTankVolume = item.quantity,
          )
          _ <- VehicleFuelItemsSql.insert.execute(dtoData)
        } yield ()
      }

    override def getByVehicleId(id: VehicleId): F[List[dto.VehicleFuelItem]] =
      VehicleFuelItemsSql.selectByVehicleId.queryList(id)

    override def findByVehicleIds(
        ids: NonEmptyList[VehicleId]
      ): F[Map[VehicleId, List[VehicleFuelItem]]] = {
      val vehicleIds = ids.toList
      VehicleFuelItemsSql
        .findByVehicleIds(vehicleIds)
        .queryList(vehicleIds)
        .map(_.map(_.toDomain).groupBy(_.vehicleId))
    }
  }
}
