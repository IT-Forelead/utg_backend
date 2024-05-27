package utg.graphql.views

import caliban.interop.cats.CatsInterop
import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.chimney.dsl.TransformationOps
import zio.query.ZQuery

import utg.Phone
import utg.algebras.AssetsAlgebra
import utg.domain.Asset.AssetInfo
import utg.domain.AssetId
import utg.domain.AuthedUser
import utg.domain.AuthedUser.{ User => UserDomain }
import utg.domain.Role
import utg.domain.UserId
import utg.graphql.DataFetcher
import utg.graphql.GraphQLContext

case class User(
    id: UserId,
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    role: Role,
    login: NonEmptyString,
    phone: Phone,
    fullName: NonEmptyString,
    image: ConsoleQuery[Option[AssetInfo]],
  )

object User {
  private def getAssetById[F[_]](
      id: AssetId
    )(implicit
      assetAlgebra: AssetsAlgebra[F],
      interop: CatsInterop[F, GraphQLContext],
    ): ConsoleQuery[AssetInfo] =
    DataFetcher.fetch[F, AssetId, AssetInfo](id, assetAlgebra.findByIds)

  def fromDomain[F[_]](
      user: UserDomain
    )(implicit
      assetAlgebra: AssetsAlgebra[F],
      interop: CatsInterop[F, GraphQLContext],
    ): User =
    user
      .into[User]
      .withFieldConst(_.image, ZQuery.foreach(user.assetId)(getAssetById[F]))
      .transform

  def fromDomain[F[_]](
      user: AuthedUser
    )(implicit
      assetAlgebra: AssetsAlgebra[F],
      interop: CatsInterop[F, GraphQLContext],
    ): User =
    user
      .into[User]
      .withFieldConst(_.image, ZQuery.foreach(user.assetId)(getAssetById[F]))
      .transform
}
