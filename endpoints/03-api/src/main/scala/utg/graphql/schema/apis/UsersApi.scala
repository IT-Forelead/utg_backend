package utg.graphql.schema.apis

import caliban.GraphQL
import caliban.RootResolver
import caliban.graphQL
import caliban.interop.cats.CatsInterop
import caliban.interop.cats.implicits._
import caliban.uploads.Uploads
import cats.MonadThrow
import cats.data.EitherT
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps

import utg.algebras.AssetsAlgebra
import utg.algebras.UsersAlgebra
import utg.domain.ResponseData
import utg.domain.UserId
import utg.domain.args.users.UpdateUserRole
import utg.domain.args.users.UserFilters
import utg.domain.args.users.UserInput
import utg.domain.enums.Privilege
import utg.exception.AError
import utg.graphql.GraphQLContext
import utg.graphql.GraphQLTypes
import utg.graphql.args.UpdateUserInput
import utg.graphql.schema.GraphQLApi
import utg.graphql.schema.Utils.Access
import utg.graphql.views.User

class UsersApi[F[_]: MonadThrow: Lambda[M[_] => CatsInterop[M, GraphQLContext]]](
    usersAlgebra: UsersAlgebra[F]
  )(implicit
    ctx: GraphQLContext,
    assetsAlgebra: AssetsAlgebra[F],
    interopUploads: CatsInterop[F, Uploads],
  ) extends GraphQLTypes
       with GraphQLApi {
  import auto._

  private case class Mutations(
      @Access(Privilege.CreateUser) createUser: UserInput => F[UserId],
      @Access(Privilege.UpdateUser) updateUser: UpdateUserInput => F[Unit],
      @Access(Privilege.UpdateAnyUser) updatePrivilege: UpdateUserRole => F[Unit],
    )

  private case class Queries(
      currentUser: F[User],
      @Access(Privilege.ViewUsers) users: UserFilters => F[ResponseData[User]],
    )

  private val mutations: Mutations = Mutations(
    createUser = userInput => usersAlgebra.create(userInput),
    updateUser = userInput =>
      for {
        user <- EitherT
          .fromOption(ctx.authInfo, AError.NotAllowed("You are not authorized"))
          .rethrowT
        fileMeta <- userInput.upload.flatTraverse(a => interopUploads.toEffect(a.meta))
        _ <- usersAlgebra.update(user.id, userInput.toDomain, fileMeta)
      } yield {},
    updatePrivilege = userPrivilege => usersAlgebra.updatePrivilege(userPrivilege),
  )

  private val queries: Queries = Queries(
    currentUser = EitherT
      .fromOption(ctx.authInfo, AError.NotAllowed("You are not authorized"))
      .map(User.fromDomain[F])
      .rethrowT,
    users =
      filter => usersAlgebra.get(filter).map(u => u.copy(data = u.data.map(User.fromDomain[F]))),
  )

  val api: GraphQL[GraphQLContext] = graphQL(RootResolver(queries, mutations))
}
