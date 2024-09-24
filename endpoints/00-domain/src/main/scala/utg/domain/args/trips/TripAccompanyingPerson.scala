package utg.domain.args.trips

import io.circe.generic.JsonCodec

import utg.domain.AuthedUser.User
import utg.domain.TripAccompanyingPersonId
import utg.domain.TripId

@JsonCodec
case class TripAccompanyingPerson(
    id: TripAccompanyingPersonId,
    tripId: TripId,
    user: Option[User],
  )
