package utg

import cats.MonadThrow
import cats.effect.std.Random
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt
import uz.scala.aws.s3.S3Client

import utg.algebras.AssetsAlgebra
import utg.algebras.UsersAlgebra
import utg.auth.impl.Auth
import utg.domain.AuthedUser
import utg.effects.Calendar
import utg.effects.GenUUID

case class Algebras[F[_]](
    auth: Auth[F, Option[AuthedUser]],
    assets: AssetsAlgebra[F],
    users: UsersAlgebra[F],
  )

object Algebras {
  def make[F[_]: MonadThrow: Calendar: GenUUID: Logger: Random: Lambda[M[_] => fs2.Compiler[M, M]]](
      auth: Auth[F, Option[AuthedUser]],
      repositories: Repositories[F],
      s3Client: S3Client[F],
    )(implicit
      P: PasswordHasher[F, SCrypt]
    ): Algebras[F] = {
    val Repositories(users, assets) = repositories
    val assetsAlgebra = AssetsAlgebra.make[F](assets, s3Client)
    Algebras[F](
      auth = auth,
      assets = assetsAlgebra,
      users = UsersAlgebra.make[F](users, assetsAlgebra),
    )
  }
}
