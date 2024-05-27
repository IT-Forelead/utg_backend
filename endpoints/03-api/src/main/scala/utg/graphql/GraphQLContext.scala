package utg.graphql

import caliban.uploads.Uploads
import utg.domain.AuthedUser
import zio.ULayer

case class GraphQLContext(
    authInfo: Option[AuthedUser],
    uploads: ULayer[Uploads]
  )

object GraphQLContext {
  val empty: GraphQLContext = GraphQLContext(None, Uploads.empty)
}
