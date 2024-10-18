package utg.repos.sql

import skunk._
import skunk.implicits._

import utg.domain.Asset
import utg.domain.AssetId

private[repos] object AssetsSql extends Sql[AssetId] {
  private val codec: Codec[Asset] = (id *: zonedDateTime *: nes *: nes.opt *: nes.opt).to[Asset]
  val insert: Command[Asset] =
    sql"""INSERT INTO assets VALUES ($codec)""".command

  def getByIds(ids: List[AssetId]): Query[ids.type, Asset] =
    sql"""SELECT * FROM assets WHERE id IN (${id.values.list(ids)})""".query(codec)

  val findById: Query[AssetId, Asset] =
    sql"""SELECT * FROM assets WHERE id = $id LIMIT 1""".query(codec)
}
