package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import utg.domain.TripDriverId
import utg.domain.TripId
import utg.domain.args.tripDrivers.TripDriverFilters
import utg.domain.args.tripDrivers.UpdateDriverExamination

private[repos] object TripDriversSql extends Sql[TripDriverId] {
  private[repos] val codec =
    (id *: TripsSql.id *: UsersSql.id *: nes *: healthType.opt *: UsersSql.id.opt
      *: AssetsSql.id.opt *: MedicalExaminationsSql.id.opt *: bool)
      .to[dto.TripDriver]

  def insert(item: List[dto.TripDriver]): Command[item.type] =
    sql"""INSERT INTO trip_drivers VALUES ${codec.values.list(item)}""".command

  val selectByTripId: Query[TripId, dto.TripDriver] =
    sql"""SELECT * FROM trip_drivers WHERE deleted = false AND trip_id = ${TripsSql.id}"""
      .query(codec)

  def findByTripIds(ids: List[TripId]): Query[ids.type, dto.TripDriver] =
    sql"""SELECT * FROM trip_drivers WHERE trip_id IN (${TripsSql.id.values.list(ids)})"""
      .query(codec)

  val update: Command[UpdateDriverExamination] =
    sql"""UPDATE trip_drivers
       SET driver_health = ${healthType.opt},
       doctor_id = ${UsersSql.id.opt},
       doctor_signature = ${AssetsSql.id.opt},
       medical_examination_id = ${MedicalExaminationsSql.id.opt}
       WHERE trip_id = ${TripsSql.id} AND driver_id = ${UsersSql.id}
     """
      .command
      .contramap {
        case ude: UpdateDriverExamination =>
          ude.driverHealth *: ude.doctorId *: ude.doctorSignature *: ude.medicalExaminationId *:
            ude.tripId *: ude.driverId *: EmptyTuple
      }

  val deleteByTripIdSql: Command[TripId] =
    sql"""DELETE FROM trip_drivers WHERE trip_id = ${TripsSql.id}""".command

  def get(filters: TripDriverFilters): AppliedFragment = {
    val searchFilters = List(
      filters.tripId.map(sql"trip_id = ${TripsSql.id}"),
      filters.driverId.map(sql"driver_id = ${UsersSql.id}"),
      filters.driverHealth.map(sql"driver_health = $healthType"),
      filters.doctorId.map(sql"doctor_id = ${UsersSql.id}"),
      filters
        .isVerified
        .map(isVerified =>
          if (isVerified) sql"""medical_examination_id IS NOT NULL""".apply(Void)
          else sql"""medical_examination_id IS NULL""".apply(Void)
        ),
    )

    val baseQuery: AppliedFragment =
      sql"""SELECT *, COUNT(*) OVER() AS total FROM trip_drivers""".apply(Void)
    baseQuery.whereAndOpt(searchFilters)
  }
}
