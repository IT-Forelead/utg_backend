package utg.routes

import java.nio.charset.StandardCharsets

import caliban.CalibanError
import caliban.CalibanError.ExecutionError
import caliban.CalibanError.ParsingError
import caliban.CalibanError.ValidationError
import caliban.GraphQLInterpreter
import caliban.GraphQLRequest
import caliban.PathValue
import caliban.ResponseValue.ObjectValue
import caliban.Value.StringValue
import caliban.uploads.FileMeta
import caliban.uploads.GraphQLUploadRequest
import caliban.uploads.Uploads
import cats.Applicative
import cats.data.OptionT
import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import utg.domain.AuthedUser
import org.http4s.AuthedRoutes
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Multipart
import uz.scala.http4s.syntax.all.deriveEntityDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.utils.Routes
import uz.scala.syntax.all.circeSyntaxDecoderOps
import zio._

import utg.Algebras
import utg.graphql.GraphQLContext
import utg.graphql.GraphQLEndpoints
import utg.effects.GenUUID
import utg.exception.AError

final class GraphQLRoutes[F[_]: JsonDecoder: Async: Dispatcher](algebras: Algebras[F])
    extends Routes[F, Option[AuthedUser]] {
  override val path: String = "/graphql"
  override val public: HttpRoutes[F] = HttpRoutes.empty
  private def parsePath(path: String): List[PathValue] = path.split('.').map(PathValue.parse).toList

  private def executeRequest(
      query: GraphQLRequest
    )(implicit
      context: GraphQLContext
    ): F[Response[F]] =
    for {
      _ <- Applicative[F].unit
      endpoints = new GraphQLEndpoints[F](algebras)

      interpreter <- endpoints.interop.toEffect(endpoints.createGraphQL.interpreter)
      result <- endpoints
        .interop
        .toEffect(withErrorCodeExtensions(interpreter).executeRequest(query))
      response <- Ok(result)
    } yield response

  override val `private`: AuthedRoutes[Option[AuthedUser], F] =
    AuthedRoutes.of[Option[AuthedUser], F] {
      case ar @ POST -> Root as authedUser =>
        ar.req.headers.get[`Content-Type`] match {
          case Some(contentType) if contentType.mediaType.isMultipart =>
            ar.req.decode[Multipart[F]] { multipart =>
              val partsMap = multipart.parts.flatMap(part => part.name.map(_ -> part)).toMap
              println("multipart")
              for {
                rawOperation <- OptionT
                  .fromOption[F](partsMap.get("operations"))
                  .getOrRaise(AError.BadRequest("Operation not found!"))
                request <- rawOperation.body.compile.toList.flatMap { bytes =>
                  new String(bytes.toArray, StandardCharsets.UTF_8).decodeAsF[F, GraphQLRequest]
                }
                rawMap <- OptionT
                  .fromOption[F](partsMap.get("map"))
                  .getOrRaise(AError.BadRequest("Map not found!"))
                map <- rawMap.body.compile.toList.flatMap { bytes =>
                  new String(bytes.toArray, StandardCharsets.UTF_8)
                    .decodeAsF[F, Map[String, List[String]]]
                }
                filePaths = map.map { case (key, value) => (key, value.flatMap(parsePath)) }.toList
                handler = Uploads.handler { handle =>
                  interop
                    .fromEffect(
                      partsMap.get(handle).traverse { fp =>
                        for {
                          uuid <- GenUUID.syncGenUUID[F].make
                          _ = println(handle)
                          fileContent <- fp.body.compile.toList
                        } yield FileMeta(
                          uuid.toString,
                          fileContent.toArray,
                          fp.contentType.map(_.mediaType).map(m => s"${m.mainType}/${m.subType}"),
                          fp.filename.getOrElse(""),
                          fileContent.size.toLong,
                        )
                      }
                    )
                    .orDie
                }
                uploadQuery = GraphQLUploadRequest(request, filePaths, handler)
                implicit0(graphQLContext: GraphQLContext) = GraphQLContext(
                  authInfo = authedUser,
                  uploads = ZLayer.fromZIO(handler),
                )

                response <- executeRequest(uploadQuery.remap)
              } yield response
            }
          case _ =>
            for {
              query <- ar.req.as[GraphQLRequest]
              implicit0(graphQLContext: GraphQLContext) = GraphQLContext(
                authInfo = authedUser,
                uploads = Uploads.empty,
              )
              response <- executeRequest(query)
            } yield response
        }
    }

  private def withErrorCodeExtensions[R](
      interpreter: GraphQLInterpreter[R, CalibanError]
    ): GraphQLInterpreter[R, CalibanError] = interpreter.mapError {
    case err @ ExecutionError(_, _, _, Some(aError: AError), _) =>
      err.copy(
        extensions = Some(ObjectValue(List("errorCode" -> StringValue(aError.errorCode)))),
        msg = aError.cause,
      )
    case err: ExecutionError =>
      err.copy(extensions = Some(ObjectValue(List("errorCode" -> StringValue("EXECUTION_ERROR")))))
    case err: ValidationError =>
      err.copy(extensions = Some(ObjectValue(List("errorCode" -> StringValue("VALIDATION_ERROR")))))
    case err: ParsingError =>
      err.copy(extensions = Some(ObjectValue(List("errorCode" -> StringValue("PARSING_ERROR")))))
  }
}
