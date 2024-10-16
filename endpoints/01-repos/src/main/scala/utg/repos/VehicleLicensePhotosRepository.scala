package utg.repos

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.AssetId
import utg.domain.VehicleId
import utg.domain.VehicleLicensePhotoId
import utg.effects.GenUUID
import utg.repos.sql.VehicleLicensePhotosSql
import utg.repos.sql.dto
import utg.utils.ID

trait VehicleLicensePhotosRepository[F[_]] {
  def create(
      vehicleId: VehicleId,
      assetIds: NonEmptyList[AssetId],
    ): F[Unit]
  def getByVehicleId(id: VehicleId): F[List[dto.VehicleLicensePhoto]]
  def findByVehicleIds(
      ids: NonEmptyList[VehicleId]
    ): F[Map[VehicleId, List[dto.VehicleLicensePhoto]]]
  def deleteByVehicleId(id: VehicleId): F[Unit]
}

object VehicleLicensePhotosRepository {
  def make[F[_]: MonadThrow: Async: GenUUID](
      implicit
      session: Resource[F, Session[F]]
    ): VehicleLicensePhotosRepository[F] = new VehicleLicensePhotosRepository[F] {
    override def create(
        vehicleId: VehicleId,
        assetIds: NonEmptyList[AssetId],
      ): F[Unit] =
      assetIds.traverse_ { assetId =>
        for {
          id <- ID.make[F, VehicleLicensePhotoId]
          dtoData = dto.VehicleLicensePhoto(
            id = id,
            vehicleId = vehicleId,
            assetId = assetId,
          )
          _ <- VehicleLicensePhotosSql.insert.execute(dtoData)
        } yield ()
      }

    override def getByVehicleId(id: VehicleId): F[List[dto.VehicleLicensePhoto]] =
      VehicleLicensePhotosSql.selectByVehicleId.queryList(id)

    override def findByVehicleIds(
        ids: NonEmptyList[VehicleId]
      ): F[Map[VehicleId, List[dto.VehicleLicensePhoto]]] = {
      val vehicleIds = ids.toList
      VehicleLicensePhotosSql
        .findByVehicleIds(vehicleIds)
        .queryList(vehicleIds)
        .map(_.groupBy(_.vehicleId))
    }

    override def deleteByVehicleId(id: VehicleId): F[Unit] =
      VehicleLicensePhotosSql.deleteByVehicleIdSql.execute(id)
  }
}
