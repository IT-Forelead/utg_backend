package utg.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all._

import utg.domain.MedicalExaminationId
import utg.domain.ResponseData
import utg.domain.args.medicalExaminations.MedicalExaminationFilters
import utg.repos.sql.MedicalExaminationsSql
import utg.repos.sql.dto

trait MedicalExaminationsRepository[F[_]] {
  def create(medicalExamination: dto.MedicalExamination): F[Unit]
  def get(filters: MedicalExaminationFilters): F[ResponseData[dto.MedicalExamination]]
  def findByIds(
      ids: List[MedicalExaminationId]
    ): F[Map[MedicalExaminationId, dto.MedicalExamination]]
}

object MedicalExaminationsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): MedicalExaminationsRepository[F] = new MedicalExaminationsRepository[F] {
    override def create(medicalExamination: dto.MedicalExamination): F[Unit] =
      MedicalExaminationsSql.insert.execute(medicalExamination)

    override def get(
        filters: MedicalExaminationFilters
      ): F[ResponseData[dto.MedicalExamination]] = {
      val af = MedicalExaminationsSql.get(filters).paginateOpt(filters.limit, filters.page)
      af.fragment
        .query(MedicalExaminationsSql.codec *: int8)
        .queryList(af.argument)
        .map { data =>
          val list = data.map(_.head)
          val count = data.headOption.fold(0L)(_.tail.head)
          ResponseData(list, count)
        }
    }

    override def findByIds(
        ids: List[MedicalExaminationId]
      ): F[Map[MedicalExaminationId, dto.MedicalExamination]] =
      NonEmptyList
        .fromList(ids)
        .fold(Map.empty[MedicalExaminationId, dto.MedicalExamination].pure[F]) { rIds =>
          val branchIds = rIds.toList
          MedicalExaminationsSql.findByIds(branchIds).queryList(branchIds).map {
            _.map { dto =>
              dto.id -> dto
            }.toMap
          }
        }
  }
}
