package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.BranchId

private[repos] object BranchesSql extends Sql[BranchId] {
  private[repos] val codec = (id *: nes *: nes *: RegionsSql.id *: bool).to[dto.Branch]

  val insert: Command[dto.Branch] =
    sql"""INSERT INTO branches VALUES ($codec)""".command

  val selectBranches: Query[Void, dto.Branch] =
    sql"""SELECT * FROM branches WHERE deleted = false ORDER BY name ASC"""
      .query(codec)
}
