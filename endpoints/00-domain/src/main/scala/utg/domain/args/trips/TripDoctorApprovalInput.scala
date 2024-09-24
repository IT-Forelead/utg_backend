package utg.domain.args.trips

import io.circe.generic.JsonCodec

import utg.domain.AssetId
import utg.domain.TripId
import utg.domain.UserId

@JsonCodec
case class TripDoctorApprovalInput(
    tripId: TripId,
    doctorId: Option[UserId],
    doctorSignature: Option[AssetId],
  )
