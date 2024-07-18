package utg.repos.sql

import eu.timepit.refined.types.string.NonEmptyString
import skunk._
import skunk.codec.all.bool
import skunk.codec.all.varchar
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.BranchId
import utg.domain.args.branches.BranchFilters

private[repos] object BranchesSql extends Sql[BranchId] {
  private[repos] val codec = (id *: nes *: nes *: RegionsSql.id *: bool).to[dto.Branch]

  val insert: Command[dto.Branch] =
    sql"""INSERT INTO branches VALUES ($codec)""".command

  val selectBranches: Query[Void, dto.Branch] =
    sql"""SELECT * FROM branches WHERE deleted = false ORDER BY name ASC"""
      .query(codec)

  def getBranches(filters: BranchFilters): AppliedFragment = {
    val searchFilters = List(
      filters.name.map(s => s"%$s%").map(sql"name ILIKE $varchar"),
      filters.code.map(s => s"%$s%").map(sql"code ILIKE $varchar"),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT * FROM branches WHERE deleted = false ORDER BY name ASC""".apply(Void)
    baseQuery.andOpt(searchFilters)
  }

  val findById: Query[BranchId, dto.Branch] =
    sql"""SELECT * FROM branches WHERE id = $id AND deleted = false LIMIT 1""".query(codec)

  def findByIds(ids: List[BranchId]): Query[ids.type, dto.Branch] =
    sql"""SELECT * FROM branches WHERE id IN (${id.values.list(ids)})""".query(codec)

  val findByCode: Query[NonEmptyString, dto.Branch] =
    sql"""SELECT * FROM branches WHERE code = $nes AND deleted = false LIMIT 1""".query(codec)

  def findByCodes(codes: List[NonEmptyString]): Query[codes.type, dto.Branch] =
    sql"""SELECT * FROM branches WHERE code in (${nes.values.list(codes)}) AND deleted = false"""
      .query(codec)

  val update: Command[dto.Branch] =
    sql"""UPDATE branches
       SET name = $nes,
       code = $nes,
       region_id = ${RegionsSql.id}
       WHERE id = $id
     """
      .command
      .contramap {
        case branch: dto.Branch =>
          branch.name *: branch.code *: branch.regionId *: branch.id *: EmptyTuple
      }
}
