package utg.repos.sql

import skunk._
import skunk.codec.all.bool
import skunk.implicits._

import utg.domain.UserId
import utg.domain.UserLicensePhotoId

private[repos] object UserLicensePhotosSql extends Sql[UserLicensePhotoId] {
  private[repos] val codec: Codec[dto.UserLicensePhoto] =
    (id *: UsersSql.id *: AssetsSql.id *: bool).to[dto.UserLicensePhoto]

  val insert: Command[dto.UserLicensePhoto] =
    sql"""INSERT INTO user_license_photos VALUES ($codec)""".command

  val selectByUserId: Query[UserId, dto.UserLicensePhoto] =
    sql"""SELECT * FROM user_license_photos WHERE deleted = false AND user_id = ${UsersSql.id}"""
      .query(codec)

  def findByUserIds(ids: List[UserId]): Query[ids.type, dto.UserLicensePhoto] =
    sql"""SELECT * FROM user_license_photos WHERE user_id IN (${UsersSql.id.values.list(ids)})"""
      .query(codec)

  val deleteByUserIdSql: Command[UserId] =
    sql"""DELETE FROM user_license_photos WHERE user_id = ${UsersSql.id}""".command
}
