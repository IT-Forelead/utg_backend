package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.CompleteTaskId
import utg.domain.args.completeTasks.CompleteTaskFilters

private[repos] object CompleteTasksSql extends Sql[CompleteTaskId] {
  private[repos] val codec =
    (id *: zonedDateTime *: TripsSql.id *: nes.opt *: nes.opt *: zonedDateTime.opt *: consignorSignId.opt *: documentId.opt *: bool)
      .to[dto.CompleteTask]

  val findById: Query[CompleteTaskId, dto.CompleteTask] =
    sql"""SELECT * FROM complete_tasks
          WHERE id = $id LIMIT 1""".query(codec)

  val insert: Command[dto.CompleteTask] =
    sql"""INSERT INTO complete_tasks VALUES ($id, ${nes.opt}, ${nes.opt}, ${zonedDateTime.opt}, ${consignorSignId.opt},, ${documentId.opt},)"""
      .command
      .contramap { (ct: dto.CompleteTask) =>
        ct.id *: ct.tripNumber *: ct.invoiceNumber *: ct.arrivalTime *: ct.consignorSignId *: ct.documentId *: EmptyTuple
      }

  val update: Command[dto.CompleteTask] =
    sql"""UPDATE complete_tasks
       SET trip_number = ${nes.opt},
       invoice_number = ${nes.opt},
       arrival_time = ${zonedDateTime.opt},
       consignor_sign_id = ${consignorSignId.opt},
       document_id = ${documentId.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case ct: dto.CompleteTask =>
          ct.tripNumber *: ct.invoiceNumber *: ct.arrivalTime *: ct.consignorSignId *: ct.documentId *: ct.id *: EmptyTuple
      }

  private def searchFilter(filters: CompleteTaskFilters): List[Option[AppliedFragment]] =
    List(
    )

  def select(filters: CompleteTaskFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT *, COUNT(*) OVER() AS total
            FROM complete_tasks WHERE deleted = false"""
    baseQuery(Void).andOpt(searchFilter(filters))
  }

  def delete: Command[CompleteTaskId] =
    sql"""DELETE FROM complete_tasks ct WHERE ct.id = $id""".command
}
