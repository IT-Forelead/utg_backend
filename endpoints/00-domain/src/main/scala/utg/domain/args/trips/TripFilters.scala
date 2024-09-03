package utg.domain.args.trips

import java.time.LocalDate
import java.time.ZonedDateTime

import eu.timepit.refined.types.numeric.NonNegInt
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.VehicleId
import utg.domain.enums.WorkingModeType

@JsonCodec
case class TripFilters(
    workingMode: Option[WorkingModeType] = None,
    startDate: Option[LocalDate] = None,
    endDate: Option[LocalDate] = None,
    vehicleId: Option[VehicleId] = None,
    from: Option[ZonedDateTime] = None,
    to: Option[ZonedDateTime] = None,
    limit: Option[NonNegInt] = None,
    offset: Option[NonNegInt] = None,
  )
