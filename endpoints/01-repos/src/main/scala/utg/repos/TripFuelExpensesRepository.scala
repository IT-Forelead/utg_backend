package utg.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripId
import utg.repos.sql.TripFuelExpensesSql
import utg.repos.sql.dto

trait TripFuelExpensesRepository[F[_]] {
  def create(input: dto.TripFuelExpense): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripFuelExpense]]
}

object TripFuelExpensesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TripFuelExpensesRepository[F] = new TripFuelExpensesRepository[F] {
    override def create(input: dto.TripFuelExpense): F[Unit] =
      TripFuelExpensesSql.insert.execute(input)

    override def getByTripId(tripId: TripId): F[List[dto.TripFuelExpense]] =
      TripFuelExpensesSql.selectByTripId.queryList(tripId)
  }
}
