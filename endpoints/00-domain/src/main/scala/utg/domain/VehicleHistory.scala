package utg.domain

import io.circe.generic.JsonCodec
import io.circe.refined._
import utg.RegisteredNumber

import java.time.ZonedDateTime

@JsonCodec
case class VehicleHistory(
    id: VehicleHistoryId,
    createdAt: ZonedDateTime,
    vehicleCategory: VehicleCategory,
    branch: Option[Branch],
    registeredNumber: Option[RegisteredNumber],
  )
