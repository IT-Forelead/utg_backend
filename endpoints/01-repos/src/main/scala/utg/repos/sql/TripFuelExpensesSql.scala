package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripFuelExpenseId
import utg.domain.TripId

private[repos] object TripFuelExpensesSql extends Sql[TripFuelExpenseId] {
  private[repos] val codec: Codec[dto.TripFuelExpense] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: nes.opt *: nes.opt *: nonNegDouble.opt *: nes.opt
      *: AssetsSql.id.opt *: nonNegDouble.opt *: nonNegDouble.opt *: nonNegDouble.opt
      *: nonNegDouble.opt *: nonNegDouble.opt *: UsersSql.id.opt *: AssetsSql.id.opt
      *: UsersSql.id.opt *: AssetsSql.id.opt *: UsersSql.id.opt *: AssetsSql.id.opt *: bool)
      .to[dto.TripFuelExpense]

  val insert: Command[dto.TripFuelExpense] =
    sql"""INSERT INTO trip_fuel_expenses VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripFuelExpense] =
    sql"""SELECT * FROM trip_fuel_expenses
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)
}
