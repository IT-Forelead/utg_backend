package utg

import caliban.CalibanError.ExecutionError
import caliban.Value.StringValue
import caliban.wrappers.Wrapper.FieldWrapper
import caliban.wrappers.Wrappers.checkDirectives
import zio.ZIO

import utg.domain.enums.Privilege
import utg.graphql.schema.Utils.attributeName
import utg.graphql.schema.Utils.directiveName
package object graphql {
  def authWrapper: FieldWrapper[GraphQLContext] =
    checkDirectives(directives =>
      for {
        userPrivileges <- ZIO.serviceWith[GraphQLContext](
          _.authInfo.map(_.role.privileges)
        )
        restrictedPrivileges = directives
          .find(_.name == directiveName)
          .flatMap(_.arguments.get(attributeName))
          .collectFirst {
            case StringValue(privileges) =>
              privileges
                .split(",")
                .flatMap(privilege => Privilege.values.find(_.entryName == privilege))
                .toList
          }
          .getOrElse(Nil)
        _ <- ZIO.unless(
          restrictedPrivileges.isEmpty || restrictedPrivileges
            .intersect(userPrivileges.toList.flatten)
            .nonEmpty
        )(
          ZIO.fail(ExecutionError("User doesn't have correct privilege"))
        )
      } yield {}
    )
}
