package utg.domain.args.tripVehicleIndicators

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.enums._

@JsonCodec
case class TripVehicleIndicatorInput(
    indicatorType: VehicleIndicatorType,
    registeredAt: ZonedDateTime,
    paidDistance: Option[NonEmptyString],
    odometerIndicator: NonEmptyString,
    currentDateTime: ZonedDateTime,
  )
