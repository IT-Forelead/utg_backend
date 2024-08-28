package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.MedicalExaminationId
import utg.domain.args.medicalExaminations.MedicalExaminationFilters

private[repos] object MedicalExaminationsSql extends Sql[MedicalExaminationId] {
  private[repos] val codec: Codec[dto.MedicalExamination] =
    (id *: zonedDateTime *: TripsSql.id *: UsersSql.id *: nonNegInt *: nes.opt *: nonNegInt *: nonNegDouble
      *: nes *: nonNegDouble *: healthType *: UsersSql.id *: AssetsSql.id *: bool)
      .to[dto.MedicalExamination]

  val insert: Command[dto.MedicalExamination] =
    sql"""INSERT INTO medical_examinations VALUES ($codec)""".command

  val findById: Query[MedicalExaminationId, dto.MedicalExamination] =
    sql"""SELECT * FROM medical_examinations WHERE id = $id LIMIT 1""".query(codec)

  def findByIds(ids: List[MedicalExaminationId]): Query[ids.type, dto.MedicalExamination] =
    sql"""SELECT * FROM medical_examinations WHERE id IN (${id.values.list(ids)})""".query(codec)

  def get(filters: MedicalExaminationFilters): AppliedFragment = {
    val searchFilters = List(
      filters.tripId.map(sql"trip_id = ${TripsSql.id}"),
      filters.driverId.map(sql"driver_id = ${UsersSql.id}"),
      filters.driverHealth.map(sql"condition = $healthType"),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT *, COUNT(*) OVER() AS total FROM medical_examinations""".apply(Void)
    baseQuery.whereAndOpt(searchFilters)
  }
}
