package utg.domain.args.vehicleHistories

import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.RegisteredNumber
import utg.domain.BranchId
import utg.domain.VehicleId

@JsonCodec
case class VehicleHistoryInput(
    vehicleId: VehicleId,
    branchId: BranchId,
    registeredNumber: Option[RegisteredNumber],
  )
