package utg.graphql.schema

import caliban.GraphQL
import utg.graphql.GraphQLContext

trait GraphQLApi {
  val api: GraphQL[GraphQLContext]
}
