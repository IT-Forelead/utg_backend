package utg.graphql.schema.apis

import caliban.GraphQL
import caliban.RootResolver
import caliban.graphQL
import caliban.interop.cats.CatsInterop
import caliban.interop.cats.implicits._
import cats.MonadThrow
import cats.implicits.toFoldableOps

import utg.auth.impl.Auth
import utg.domain.AuthedUser
import utg.domain.auth.AuthTokens
import utg.domain.auth.Credentials
import utg.graphql.GraphQLContext
import utg.graphql.GraphQLTypes
import utg.graphql.schema.GraphQLApi

class AuthApi[F[_]: MonadThrow: Lambda[M[_] => CatsInterop[M, GraphQLContext]]](
    auth: Auth[F, Option[AuthedUser]]
  )(implicit
    ctx: GraphQLContext
  ) extends GraphQLTypes
       with GraphQLApi {
  import auto._

  private case class Mutations(
      login: Credentials => F[AuthTokens],
      refreshToken: String => F[AuthTokens],
    )
  private case class Queries(
      logout: F[Unit]
    )
  private val mutations: Mutations = Mutations(
    login = credentials => auth.login(credentials),
    refreshToken = token => auth.refresh(token),
  )
  private val queries: Queries = Queries(
    logout = ctx.authInfo.traverse_(user => auth.destroySession(user.login))
  )

  val api: GraphQL[GraphQLContext] = graphQL(RootResolver(queries, mutations))
}
