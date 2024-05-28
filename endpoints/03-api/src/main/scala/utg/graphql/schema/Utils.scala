package utg.graphql.schema

import caliban.Value.StringValue
import caliban.parsing.adt.Directive
import caliban.schema.Annotations.GQLDirective

import utg.domain.enums.Privilege
object Utils {
  val directiveName = "requiredPrivilege"
  val attributeName = "privilege"

  case class Access(privilege: Privilege*)
      extends GQLDirective(
        Directive(
          directiveName,
          Map(attributeName -> StringValue(privilege.map(_.entryName).mkString(","))),
        )
      )
}
