package utg.repos.sql

import shapeless.HNil
import skunk._
import skunk.codec.all.varchar
import skunk.implicits._
import utg.domain.CompleteTaskId
import utg.domain.args.completeTasks.CompleteTaskFilters
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

private[repos] object CompleteTasksSql extends Sql[CompleteTaskId] {
  private[repos] val codec =
    (id *: zonedDateTime *: nes *: nes *: nes.opt *: phone *: RolesSql.id *: AssetsSql
      .id
      .opt *: nes.opt)
      .to[dto.CompleteTask]

  val findById: Query[CompleteTaskId, dto.CompleteTask] =
    sql"""SELECT id, created_at, firstname, lastname, middle_name, phone, role_id, asset_id, branch_code FROM complete_tasks
          WHERE id = $id LIMIT 1""".query(codec)

  val insert: Command[dto.CompleteTask] =
    sql"""INSERT INTO complete_tasks VALUES ($id, $zonedDateTime, $nes, $nes, ${nes.opt}, $phone, ${RolesSql.id}, ${AssetsSql
        .id
        .opt}, ${nes.opt}, $passwordHash)"""
      .command
      .contramap { (ct: dto.CompleteTask) =>
        ct.data.id *: ct.data.createdAt *: ct.data.firstname *: ct.data.lastname *: ct.data.middleName *:
          ct.data.phone *: ct
            .data
            .roleId *: ct.data.assetId *: ct.data.branchCode *: ct.password *: EmptyTuple
      }

  val update: Command[dto.CompleteTask] =
    sql"""UPDATE complete_tasks
       SET firstname = $nes,
       lastname = $nes,
       middle_name = ${nes.opt},
       phone = $phone,
       role_id = ${RolesSql.id},
       asset_id = ${AssetsSql.id.opt},
       branch_code = ${nes.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case complete_task: dto.CompleteTask =>
          complete_task.firstname *: complete_task.lastname *: complete_task.middleName *: complete_task.phone *: complete_task.roleId *: complete_task.assetId *: complete_task.branchCode *: complete_task.id *: EmptyTuple
      }

  
  private def searchFilter(filters: CompleteTaskFilters): List[Option[AppliedFragment]] =
    List(
    )

  

  def select(filters: CompleteTaskFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT *,
              COUNT(*) OVER() AS total
            FROM complete_tasks ct"""
    baseQuery(Void).whereAndOpt(searchFilter(filters))
  }

  def delete: Command[CompleteTaskId] =
    sql"""DELETE FROM complete_tasks ct WHERE ct.id = $id""".command
}
