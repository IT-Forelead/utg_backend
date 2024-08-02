package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.TripFuelExpenseId
import utg.domain.TripId

private[repos] object TripFuelExpensesSql extends Sql[TripFuelExpenseId] {
  private[repos] val codec: Codec[dto.TripFuelExpense] =
    (id *: zonedDateTime *: TripsSql.id *: VehiclesSql.id *: nes.opt *: nes.opt *: nonNegDouble.opt
      *: UsersSql.id.opt *: AssetsSql.id.opt *: nonNegDouble.opt *: nonNegDouble.opt
      *: nonNegDouble.opt *: nonNegDouble.opt *: nonNegDouble.opt *: UsersSql.id.opt
      *: AssetsSql.id.opt *: UsersSql.id.opt *: AssetsSql.id.opt *: UsersSql.id.opt
      *: AssetsSql.id.opt *: bool).to[dto.TripFuelExpense]

  val insert: Command[dto.TripFuelExpense] =
    sql"""INSERT INTO trip_fuel_expenses VALUES ($codec)""".command

  val selectByTripId: Query[TripId, dto.TripFuelExpense] =
    sql"""SELECT * FROM trip_fuel_expenses
         WHERE deleted = false AND trip_id = ${TripsSql.id}
         ORDER BY created_at DESC""".query(codec)

  val findById: Query[TripFuelExpenseId, dto.TripFuelExpense] =
    sql"""SELECT * FROM trip_fuel_expenses WHERE deleted = false AND id = $id LIMIT 1""".query(
      codec
    )

  val update: Command[dto.TripFuelExpense] =
    sql"""UPDATE trip_fuel_expenses
       SET trip_id = ${TripsSql.id},
       vehicle_id = ${VehiclesSql.id},
       fuel_brand = ${nes.opt},
       brand_code = ${nes.opt},
       fuel_given = ${nonNegDouble.opt},
       refueler_id = ${UsersSql.id.opt},
       refueler_signature = ${AssetsSql.id.opt},
       fuel_in_tank = ${nonNegDouble.opt},
       fuel_remaining = ${nonNegDouble.opt},
       norm_change_coeff = ${nonNegDouble.opt},
       equipment_working_time = ${nonNegDouble.opt},
       engine_working_time = ${nonNegDouble.opt},
       tank_check_mechanic = ${UsersSql.id.opt},
       tank_check_mechanic_signature = ${AssetsSql.id.opt},
       remaining_check_mechanic = ${UsersSql.id.opt},
       remaining_check_mechanic_signature = ${AssetsSql.id.opt},
       dispatcher = ${UsersSql.id.opt},
       dispatcher_signature = ${AssetsSql.id.opt}
       WHERE id = $id
     """
      .command
      .contramap {
        case tfe: dto.TripFuelExpense =>
          tfe.tripId *: tfe.vehicleId *: tfe.fuelBrand *: tfe.brandCode *: tfe.fuelGiven *: tfe.refuelerId *:
            tfe.attendantSignature *: tfe.fuelInTank *: tfe.fuelRemaining *: tfe.normChangeCoefficient *:
            tfe.equipmentWorkingTime *: tfe.engineWorkingTime *: tfe.tankCheckMechanicId *:
            tfe.tankCheckMechanicSignature *: tfe.remainingCheckMechanicId *: tfe.remainingCheckMechanicSignature *:
            tfe.dispatcherId *: tfe.dispatcherSignature *: tfe.id *: EmptyTuple
      }
}
