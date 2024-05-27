package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFunctorOps
import utg.domain.Asset
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
import utg.repos.sql.AssetsSql
import utg.domain.{AssetId}

trait AssetsRepository[F[_]] {
  def create(asset: Asset): F[Unit]
  def getAssets(assetIds: NonEmptyList[AssetId]): F[Map[AssetId, Asset]]
  def findAsset(assetId: AssetId): F[Option[Asset]]
}

object AssetsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): AssetsRepository[F] = new AssetsRepository[F] {
    override def create(asset: Asset): F[Unit] =
      AssetsSql.insert.execute(asset)

    override def getAssets(assetIds: NonEmptyList[AssetId]): F[Map[AssetId, Asset]] = {
      val assets = assetIds.toList
      AssetsSql.getByIds(assets).queryList(assets).map(_.map(a => a.id -> a).toMap)
    }

    override def findAsset(assetId: AssetId): F[Option[Asset]] =
      AssetsSql.findById.queryOption(assetId)
  }
}
