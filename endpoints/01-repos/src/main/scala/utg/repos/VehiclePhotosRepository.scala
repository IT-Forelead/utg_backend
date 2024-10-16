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
import utg.domain.VehiclePhotoId
import utg.effects.GenUUID
import utg.repos.sql.VehiclePhotosSql
import utg.repos.sql.dto
import utg.utils.ID

trait VehiclePhotosRepository[F[_]] {
  def create(
      vehicleId: VehicleId,
      assetIds: NonEmptyList[AssetId],
    ): F[Unit]
  def getByVehicleId(id: VehicleId): F[List[dto.VehiclePhoto]]
  def findByVehicleIds(ids: NonEmptyList[VehicleId]): F[Map[VehicleId, List[dto.VehiclePhoto]]]
  def deleteByVehicleId(id: VehicleId): F[Unit]
}

object VehiclePhotosRepository {
  def make[F[_]: MonadThrow: Async: GenUUID](
      implicit
      session: Resource[F, Session[F]]
    ): VehiclePhotosRepository[F] = new VehiclePhotosRepository[F] {
    override def create(
        vehicleId: VehicleId,
        assetIds: NonEmptyList[AssetId],
      ): F[Unit] =
      assetIds.traverse_ { assetId =>
        for {
          id <- ID.make[F, VehiclePhotoId]
          dtoData = dto.VehiclePhoto(
            id = id,
            vehicleId = vehicleId,
            assetId = assetId,
          )
          _ <- VehiclePhotosSql.insert.execute(dtoData)
        } yield ()
      }

    override def getByVehicleId(id: VehicleId): F[List[dto.VehiclePhoto]] =
      VehiclePhotosSql.selectByVehicleId.queryList(id)

    override def findByVehicleIds(
        ids: NonEmptyList[VehicleId]
      ): F[Map[VehicleId, List[dto.VehiclePhoto]]] = {
      val vehicleIds = ids.toList
      VehiclePhotosSql
        .findByVehicleIds(vehicleIds)
        .queryList(vehicleIds)
        .map(_.groupBy(_.vehicleId))
    }

    override def deleteByVehicleId(id: VehicleId): F[Unit] =
      VehiclePhotosSql.deleteByVehicleIdSql.execute(id)
  }
}
