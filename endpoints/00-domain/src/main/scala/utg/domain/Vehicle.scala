package utg.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.all.NonNegDouble
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.RegisteredNumber
import utg.domain.enums.VehicleType

@JsonCodec
case class Vehicle(
    id: VehicleId,
    createdAt: ZonedDateTime,
    name: NonEmptyString,
    registeredNumber: RegisteredNumber,
    vehicleType: VehicleType,
    fuelTankVolume: NonNegDouble,
  )
