package utg.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.toFunctorOps
import skunk.Session
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV
import utg.domain.{ResponseData, TripDriverTask, TripDriverTaskId, TripId}
import utg.domain.args.tripDriverTasks.TripDriverTaskFilters
import utg.exception.AError
import utg.repos.sql.TripDriverTasksSql
import utg.repos.sql.dto

trait TripDriverTasksRepository[F[_]] {
  def findById(id: TripDriverTaskId): F[Option[TripDriverTask]]
  def create(tripDriverTaskAndHash: dto.TripDriverTask): F[Unit]
  def update(id: TripDriverTaskId)(update: dto.TripDriverTask => dto.TripDriverTask): F[Unit]
  def delete(id: TripDriverTaskId): F[Unit]
  def get(filters: TripDriverTaskFilters): F[ResponseData[TripDriverTask]]
  def getAsStream(filters: TripDriverTaskFilters): fs2.Stream[F, dto.TripDriverTask]
  def makeTripDriverTask(tripDriverTaskDto: dto.TripDriverTask): TripDriverTask
  def getByTripId(tripId: TripId): F[List[dto.TripDriverTask]]
}

object TripDriverTasksRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripDriverTasksRepository[F] = new TripDriverTasksRepository[F] {
    private def makeTripDriverTasks(dtos: List[dto.TripDriverTask]): List[TripDriverTask] =
      dtos.map { tripDriverTaskDto =>
        tripDriverTaskDto.toDomain
      }

    def makeTripDriverTask(tripDriverTaskDto: dto.TripDriverTask): TripDriverTask =
      tripDriverTaskDto.toDomain

    override def findById(id: TripDriverTaskId): F[Option[TripDriverTask]] =
      OptionT(TripDriverTasksSql.findById.queryOption(id)).map(makeTripDriverTask).value

    override def create(tripDriverTask: dto.TripDriverTask): F[Unit] =
      TripDriverTasksSql.insert.execute(tripDriverTask)

    override def get(filters: TripDriverTaskFilters): F[ResponseData[TripDriverTask]] = {
      val af = TripDriverTasksSql
        .get(filters)
        .paginateOpt(filters.limit.map(_.value), filters.page.map(_.value))
      af.fragment.query(TripDriverTasksSql.codec *: int8).queryList(af.argument).map {
        tripDriverTasksDto =>
          ResponseData(
            makeTripDriverTasks(tripDriverTasksDto.map(_.head)),
            tripDriverTasksDto.headOption.fold(0L)(_.tail.head),
          )
      }
    }

    override def getByTripId(tripId: TripId): F[List[dto.TripDriverTask]] =
      TripDriverTasksSql.selectByTripId.queryList(tripId)

    override def update(
        id: TripDriverTaskId
      )(
        update: dto.TripDriverTask => dto.TripDriverTask
      ): F[Unit] =
      OptionT(TripDriverTasksSql.findById.queryOption(id)).cataF(
        AError.Internal(s"TripDriverTask not found by id [$id]").raiseError[F, Unit],
        tripDriverTask => TripDriverTasksSql.update.execute(update(tripDriverTask)),
      )

    override def delete(id: TripDriverTaskId): F[Unit] =
      TripDriverTasksSql.delete.execute(id)

    override def getAsStream(filters: TripDriverTaskFilters): fs2.Stream[F, dto.TripDriverTask] = {
      val af =
        TripDriverTasksSql
          .get(filters)
          .paginateOpt(filters.limit.map(_.value), filters.page.map(_.value))
      af.fragment.query(TripDriverTasksSql.codec *: int8).queryStream(af.argument).map(_._1)
    }
  }
}
