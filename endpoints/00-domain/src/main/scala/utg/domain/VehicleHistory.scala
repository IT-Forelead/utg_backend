package utg.domain

import java.time.ZonedDateTime

import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.RegisteredNumber

@JsonCodec
case class VehicleHistory(
    id: VehicleHistoryId,
    createdAt: ZonedDateTime,
    vehicleCategory: VehicleCategory,
    branch: Option[Branch],
    registeredNumber: Option[RegisteredNumber],
  )
