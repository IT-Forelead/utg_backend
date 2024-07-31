package utg.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.toFunctorOps
import skunk.Session
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.domain.LineDelay
import utg.domain.LineDelayId
import utg.domain.ResponseData
import utg.domain.args.lineDelays.LineDelayFilters
import utg.exception.AError
import utg.repos.sql.LineDelaysSql
import utg.repos.sql.dto

trait LineDelaysRepository[F[_]] {
  def findById(id: LineDelayId): F[Option[LineDelay]]
  def create(lineDelay: dto.LineDelay): F[Unit]
  def update(id: LineDelayId)(update: dto.LineDelay => dto.LineDelay): F[Unit]
  def delete(id: LineDelayId): F[Unit]
  def get(filters: LineDelayFilters): F[ResponseData[LineDelay]]
}

object LineDelaysRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): LineDelaysRepository[F] = new LineDelaysRepository[F] {
    private def makeLineDelay(lineDelay: dto.LineDelay): LineDelay =
      lineDelay.toDomain

    private def makeLineDelays(lineDelays: List[dto.LineDelay]): List[LineDelay] =
      lineDelays.map { lineDelay =>
        lineDelay.toDomain
      }

    override def findById(id: LineDelayId): F[Option[LineDelay]] =
      OptionT(LineDelaysSql.findById.queryOption(id)).map(makeLineDelay).value

    override def create(lineDelay: dto.LineDelay): F[Unit] =
      LineDelaysSql.insert.execute(lineDelay)

    override def get(filters: LineDelayFilters): F[ResponseData[LineDelay]] = {
      val af = LineDelaysSql
        .select(filters)
        .paginateOpt(filters.limit.map(_.value), filters.page.map(_.value))
      af.fragment.query(LineDelaysSql.codec *: int8).queryList(af.argument).map { lineDelaysDto =>
        ResponseData(
          makeLineDelays(lineDelaysDto.map(_.head)),
          lineDelaysDto.headOption.fold(0L)(_.tail.head),
        )
      }
    }

    override def update(id: LineDelayId)(update: dto.LineDelay => dto.LineDelay): F[Unit] =
      OptionT(LineDelaysSql.findById.queryOption(id)).cataF(
        AError.Internal(s"LineDelay not found by id [$id]").raiseError[F, Unit],
        lineDelay => LineDelaysSql.update.execute(update(lineDelay)),
      )

    override def delete(id: LineDelayId): F[Unit] =
      LineDelaysSql.delete.execute(id)
  }
}
