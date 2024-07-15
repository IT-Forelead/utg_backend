package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.toFunctorOps
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import utg.domain.Region
import utg.domain.RegionId
import utg.repos.sql.RegionsSql

trait RegionsRepository[F[_]] {
  def getRegions: F[List[Region]]
  def findByIds(ids: List[RegionId]): F[Map[RegionId, Region]]
}

object RegionsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): RegionsRepository[F] = new RegionsRepository[F] {
    override def getRegions: F[List[Region]] = for {
      dtoRegions <- RegionsSql.selectRegions.queryList(Void)
      regions = dtoRegions.map { region =>
        Region(
          id = region.id,
          name = region.name,
        )
      }
    } yield regions

    override def findByIds(ids: List[RegionId]): F[Map[RegionId, Region]] =
      NonEmptyList.fromList(ids).fold(Map.empty[RegionId, Region].pure[F]) { rIds =>
        val regionIds = rIds.toList
        RegionsSql.findByIds(regionIds).queryList(regionIds).map {
          _.map { dtoRegion =>
            dtoRegion.id -> Region(dtoRegion.id, dtoRegion.name)
          }.toMap
        }
      }
  }
}
