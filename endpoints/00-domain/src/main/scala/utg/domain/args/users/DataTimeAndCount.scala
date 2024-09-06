package utg.domain.args.users

import java.time.LocalDateTime

import io.circe.generic.JsonCodec

@JsonCodec
case class DataTimeAndCount(
    datetime: LocalDateTime,
    total: Int = 0,
  )
