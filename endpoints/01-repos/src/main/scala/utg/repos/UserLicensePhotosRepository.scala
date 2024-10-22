package utg.repos

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.AssetId
import utg.domain.UserId
import utg.domain.UserLicensePhotoId
import utg.effects.GenUUID
import utg.repos.sql.UserLicensePhotosSql
import utg.repos.sql.dto
import utg.utils.ID

trait UserLicensePhotosRepository[F[_]] {
  def create(
      userId: UserId,
      assetIds: NonEmptyList[AssetId],
    ): F[Unit]
  def getByUserId(id: UserId): F[List[dto.UserLicensePhoto]]
  def findByUserIds(
      ids: NonEmptyList[UserId]
    ): F[Map[UserId, List[dto.UserLicensePhoto]]]
  def deleteByUserId(id: UserId): F[Unit]
}

object UserLicensePhotosRepository {
  def make[F[_]: MonadThrow: Async: GenUUID](
      implicit
      session: Resource[F, Session[F]]
    ): UserLicensePhotosRepository[F] = new UserLicensePhotosRepository[F] {
    override def create(
        userId: UserId,
        assetIds: NonEmptyList[AssetId],
      ): F[Unit] =
      assetIds.traverse_ { assetId =>
        for {
          id <- ID.make[F, UserLicensePhotoId]
          dtoData = dto.UserLicensePhoto(
            id = id,
            userId = userId,
            assetId = assetId,
          )
          _ <- UserLicensePhotosSql.insert.execute(dtoData)
        } yield ()
      }

    override def getByUserId(id: UserId): F[List[dto.UserLicensePhoto]] =
      UserLicensePhotosSql.selectByUserId.queryList(id)

    override def findByUserIds(
        ids: NonEmptyList[UserId]
      ): F[Map[UserId, List[dto.UserLicensePhoto]]] = {
      val userIds = ids.toList
      UserLicensePhotosSql
        .findByUserIds(userIds)
        .queryList(userIds)
        .map(_.groupBy(_.userId))
    }

    override def deleteByUserId(id: UserId): F[Unit] =
      UserLicensePhotosSql.deleteByUserIdSql.execute(id)
  }
}
