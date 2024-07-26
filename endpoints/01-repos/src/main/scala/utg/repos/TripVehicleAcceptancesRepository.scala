package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripVehicleAcceptancesSql
import utg.repos.sql.dto

trait TripVehicleAcceptancesRepository[F[_]] {
  def create(input: dto.TripVehicleAcceptance): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripVehicleAcceptance]]
}

object TripVehicleAcceptancesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripVehicleAcceptancesRepository[F] = new TripVehicleAcceptancesRepository[F] {
    override def create(input: dto.TripVehicleAcceptance): F[Unit] =
      TripVehicleAcceptancesSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripVehicleAcceptance]] =
      TripVehicleAcceptancesSql.selectByTripId.queryList(tripId)
  }
}
