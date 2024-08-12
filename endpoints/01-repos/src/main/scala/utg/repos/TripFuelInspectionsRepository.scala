package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripFuelInspectionsSql
import utg.repos.sql.dto

trait TripFuelInspectionsRepository[F[_]] {
  def create(input: dto.TripFuelInspection): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripFuelInspection]]
}

object TripFuelInspectionsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripFuelInspectionsRepository[F] = new TripFuelInspectionsRepository[F] {
    override def create(input: dto.TripFuelInspection): F[Unit] =
      TripFuelInspectionsSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripFuelInspection]] =
      TripFuelInspectionsSql.selectByTripId.queryList(tripId)
  }
}
