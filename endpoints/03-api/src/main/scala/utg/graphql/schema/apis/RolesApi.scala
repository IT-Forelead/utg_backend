package utg.graphql.schema.apis

import caliban.GraphQL
import caliban.RootResolver
import caliban.graphQL
import caliban.interop.cats.CatsInterop
import caliban.interop.cats.implicits._
import cats.MonadThrow

import utg.algebras.RolesAlgebra
import utg.domain.Role
import utg.graphql.GraphQLContext
import utg.graphql.GraphQLTypes
import utg.graphql.schema.GraphQLApi

class RolesApi[F[_]: MonadThrow: Lambda[M[_] => CatsInterop[M, GraphQLContext]]](
    rolesAlgebra: RolesAlgebra[F]
  )(implicit
    ctx: GraphQLContext
  ) extends GraphQLTypes
       with GraphQLApi {
  import auto._

  private case class Queries(
      roles: F[List[Role]]
    )

  private val queries: Queries = Queries(
    roles = rolesAlgebra.getAll
  )

  val api: GraphQL[GraphQLContext] = graphQL(RootResolver(queries))
}
