package utg.domain.args.trips

import io.circe.generic.JsonCodec
import io.circe.refined._

import utg.domain.AuthedUser.User
import utg.domain.TripAccompanyingPersonId
import utg.domain.TripId
import utg.domain.UserId

@JsonCodec
case class TripAccompanyingPerson(
    id: TripAccompanyingPersonId,
    tripId: TripId,
    user: Option[User],
  )
