package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.RegionId

private[repos] object RegionsSql extends Sql[RegionId] {
  private[repos] val codec = (id *: nes *: bool).to[dto.Region]

  val selectRegions: Query[Void, dto.Region] =
    sql"""SELECT * FROM regions WHERE deleted = false ORDER BY name ASC"""
      .query(codec)

  def findByIds(ids: List[RegionId]): Query[ids.type, dto.Region] =
    sql"""SELECT * FROM regions WHERE id IN (${id.values.list(ids)})""".query(codec)

  val findById: Query[RegionId, dto.Region] =
    sql"""SELECT * FROM regions WHERE id=$id""".query(codec)
}
