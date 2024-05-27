package utg.graphql

import caliban.GraphQL
import caliban.interop.cats.CatsInterop
import caliban.uploads.Uploads
import caliban.wrappers.DeferSupport
import caliban.wrappers.Wrappers._
import cats.effect.Async
import cats.effect.std.Dispatcher
import zio.Runtime
import zio.Unsafe
import zio.ZEnvironment
import zio.durationInt

import utg.Algebras
import utg.algebras.AssetsAlgebra
import utg.algebras.RolesAlgebra
import utg.algebras.UsersAlgebra
import utg.auth.impl.Auth
import utg.domain.AuthedUser
import utg.graphql.schema.GraphQLApi
import utg.graphql.schema.apis.AuthApi
import utg.graphql.schema.apis.RolesApi
import utg.graphql.schema.apis.UsersApi
class GraphQLEndpoints[F[_]: Async](
    algebras: Algebras[F]
  )(implicit
    dispatcher: Dispatcher[F],
    ctx: GraphQLContext,
  ) {
  implicit val Algebras(
    auth: Auth[F, Option[AuthedUser]],
    assets: AssetsAlgebra[F],
    users: UsersAlgebra[F],
    roles: RolesAlgebra[F],
  ) = algebras

  implicit val runtime: Runtime[GraphQLContext] =
    Runtime.default.withEnvironment(ZEnvironment(ctx))
  implicit val interop: CatsInterop[F, GraphQLContext] =
    CatsInterop.default[F, GraphQLContext](dispatcher)
  implicit val runtimeUpload: Runtime[Uploads] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.unsafe.fromLayer(ctx.uploads)
    }
  implicit val interopUploads: CatsInterop[F, Uploads] =
    CatsInterop.default[F, Uploads](dispatcher)
  private val apis: List[GraphQLApi] =
    List(
      new AuthApi[F](algebras.auth),
      new UsersApi(algebras.users),
      new RolesApi(algebras.roles),
    )

  def createGraphQL: GraphQL[GraphQLContext] =
    apis.map(_.api).reduce(_ |+| _) @@
      maxDepth(50) @@
      timeout(30.seconds) @@
      printSlowQueries(10.seconds) @@
      authWrapper @@
      DeferSupport.defer @@
      printErrors
}
