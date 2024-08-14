package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User

@JsonCodec
case class TripFuelRate(
    id: TripFuelRateId,
    createdAt: ZonedDateTime,
    tripId: TripId,
    normChangeCoefficient: NonNegDouble,
    equipmentWorkingTime: NonNegDouble,
    engineWorkingTime: NonNegDouble,
    dispatcher: Option[User],
    dispatcherSignature: AssetId,
  )
