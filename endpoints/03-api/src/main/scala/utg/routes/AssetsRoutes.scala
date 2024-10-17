package utg.routes

import java.util.Base64

import cats.effect.Async
import cats.effect.std.Random
import cats.implicits._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.multipart.Multipart
import org.typelevel.log4cats.Logger
import uz.scala.http4s.syntax.all._
import uz.scala.http4s.utils.Routes

import utg.algebras.AssetsAlgebra
import utg.domain.AuthedUser
import utg.domain.FileMeta
import utg.domain.args.users.AssetInput
import utg.randomStr

final case class AssetsRoutes[F[_]: Logger: JsonDecoder: Async: Random](
    assetsAlgebra: AssetsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/assets"
  private val AllowedMediaTypes: List[MediaType] = List(
    MediaType.image.png,
    MediaType.image.jpeg,
  )

  private def decode(base64String: String): Array[Byte] = {
    val decoder = Base64.getDecoder
    decoder.decode(base64String)
  }

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[AssetInput] { assetInput =>
        for {
          id <- randomStr[F](4)
          fileName = s"image-$id.png"
          base64 = assetInput.base64.value
          bytes = decode(base64.split(",")(1))
          streamBytes = fs2.Stream.iterable(bytes).covary[F]
          contentType = Option(base64.split(",")(0).split(";")(0).replace("data:", ""))
          fileMeta = FileMeta(
            bytes = streamBytes,
            contentType = contentType,
            fileName = fileName,
            fileSize = bytes.length.toLong,
          )
          assetId <- assetsAlgebra.create(fileMeta)
          response <- Ok(assetId.value.toString)
        } yield response
      }

    case ar @ POST -> Root / "create" as _ =>
      ar.req.decode[Multipart[F]] { multipart =>
        val fileParts = multipart.parts.fileParts(AllowedMediaTypes: _*)
        val fileMeta = fileParts.map { fp =>
          FileMeta(
            fp.body,
            fp.contentType.map(_.mediaType).map(m => s"${m.mainType}/${m.subType}"),
            fp.filename.getOrElse(""),
            fp.contentLength.getOrElse(0L),
          )
        }
        fileMeta.traverse(fm => assetsAlgebra.create(fm)).flatMap(Created(_))
      }
  }
}
