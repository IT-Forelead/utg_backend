package utg.algebras

import java.net.URL

import caliban.uploads.FileMeta
import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import uz.scala.aws.s3.S3Client
import uz.scala.syntax.refined.commonSyntaxAutoRefineOptV
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.domain.Asset
import utg.domain.Asset.AssetInfo
import utg.domain.AssetId
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.exception.AError
import utg.repos.AssetsRepository
import utg.utils.ID

trait AssetsAlgebra[F[_]] {
  def create(meta: FileMeta): F[AssetId]
  def getPublicUrl(assetIds: NonEmptyList[AssetId]): F[Map[AssetId, URL]]
  def getAssetInfo(assetId: AssetId): F[AssetInfo]
  def findByIds(assetIds: List[AssetId]): F[Map[AssetId, AssetInfo]]
}
object AssetsAlgebra {
  def make[F[_]: MonadThrow: GenUUID: Calendar: Lambda[M[_] => fs2.Compiler[M, M]]](
      assetsRepository: AssetsRepository[F],
      s3Client: S3Client[F],
    ): AssetsAlgebra[F] =
    new AssetsAlgebra[F] {
      override def create(meta: FileMeta): F[AssetId] =
        for {
          id <- ID.make[F, AssetId]
          now <- Calendar[F].currentZonedDateTime
          key <- genFileKey(meta.fileName)

          asset = Asset(
            id = id,
            createdAt = now,
            s3Key = key,
            fileName = meta.fileName.some,
            contentType = meta.contentType,
          )
          _ <- fs2.Stream.iterable(meta.bytes).through(s3Client.putObject(key)).compile.drain
          _ <- assetsRepository.create(asset)
        } yield id

      override def getPublicUrl(assetIds: NonEmptyList[AssetId]): F[Map[AssetId, URL]] =
        for {
          assets <- assetsRepository.getAssets(assetIds)
          assetUrls <- assets.toList.traverse {
            case assetId -> asset =>
              s3Client.generateUrl(asset.s3Key.value).map(assetId -> _)
          }
        } yield assetUrls.toMap

      override def getAssetInfo(assetId: AssetId): F[AssetInfo] =
        OptionT(assetsRepository.findAsset(assetId)).foldF(
          AError.Internal(s"File not found by assetId [$assetId]").raiseError[F, AssetInfo]
        ) { asset =>
          s3Client.generateUrl(asset.s3Key.value).map { url =>
            AssetInfo(
              asset.fileName,
              asset.contentType,
              getFileType(asset.s3Key.value),
              url,
            )
          }
        }

      override def findByIds(assetIds: List[AssetId]): F[Map[AssetId, AssetInfo]] =
        for {
          assets <- NonEmptyList
            .fromList(assetIds)
            .fold(Map.empty[AssetId, Asset].pure[F])(assetsRepository.getAssets)
          assetInfoById <- assets.toList.traverse {
            case assetId -> asset =>
              s3Client.generatePresignedUrl(asset.s3Key.value).map { url =>
                assetId -> AssetInfo(
                  asset.fileName,
                  asset.contentType,
                  getFileType(asset.s3Key.value),
                  url,
                )
              }
          }
        } yield assetInfoById.toMap

      private def getFileType(filename: String): String = {
        val extension = filename.substring(filename.lastIndexOf('.') + 1)
        extension.toLowerCase
      }

      private def genFileKey(orgFilename: String): F[String] =
        GenUUID[F].make.map { uuid =>
          uuid.toString + "." + getFileType(orgFilename)
        }
    }
}
