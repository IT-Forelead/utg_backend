package utg.routes

import java.util.Base64
import caliban.uploads.FileMeta
import cats.MonadThrow
import cats.effect.std.Random
import cats.implicits._
import cats.implicits.toFlatMapOps
import io.circe.syntax.EncoderOps
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes
import utg.algebras.AssetsAlgebra
import utg.domain.AuthedUser
import utg.domain.args.users.AssetInput
import utg.randomStr

final case class AssetsRoutes[F[_]: Logger: JsonDecoder: MonadThrow: Random](
    assetsAlgebra: AssetsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/assets"

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
          _ = println(base64.split(",")(0))

          contentType = Option(base64.split(",")(0).split(";")(0).replace("data:", ""))
          _ = println(contentType)
          fileMeta = FileMeta(
            id = id,
            bytes = bytes,
            contentType = contentType,
            fileName = fileName,
            fileSize = bytes.length.toLong,
          )
          assetId <- assetsAlgebra.create(fileMeta)
          response <- Ok(("assetId" -> assetId.value.toString).asJson)
        } yield response
      }
  }
}
