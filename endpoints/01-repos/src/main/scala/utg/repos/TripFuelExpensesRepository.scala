package utg.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.TripFuelExpenseId
import utg.domain.TripId
import utg.exception.AError
import utg.repos.sql.TripFuelExpensesSql
import utg.repos.sql.dto

trait TripFuelExpensesRepository[F[_]] {
  def create(input: dto.TripFuelExpense): F[Unit]
  def getByTripId(tripId: TripId): F[List[dto.TripFuelExpense]]
  def update(id: TripFuelExpenseId)(update: dto.TripFuelExpense => dto.TripFuelExpense): F[Unit]
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

    override def update(
        id: TripFuelExpenseId
      )(
        update: dto.TripFuelExpense => dto.TripFuelExpense
      ): F[Unit] =
      OptionT(TripFuelExpensesSql.findById.queryOption(id)).cataF(
        AError.Internal(s"Trip fuel expense not found by id [$id]").raiseError[F, Unit],
        tfe => TripFuelExpensesSql.update.execute(update(tfe)),
      )
  }
}
