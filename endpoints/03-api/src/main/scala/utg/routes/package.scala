package utg

import scala.util.Try

import org.http4s.ParseFailure
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

package object routes {
  implicit val phoneDecoder: QueryParamDecoder[Phone] =
    QueryParamDecoder[String].emap[Phone](str =>
      Try(str: Phone)
        .fold(
          error => Left(ParseFailure("Incorrect phone number", error.getMessage)),
          Right(_),
        )
    )

  object PhoneParam extends QueryParamDecoderMatcher[Phone]("phone")
}
