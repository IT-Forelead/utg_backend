package utg.algebras

import caliban.uploads.FileMeta
import cats.Applicative
import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.std.Random
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt
import uz.scala.integration.sms.OperSmsClient
import uz.scala.syntax.refined._

import utg.domain.AuthedUser.User
import utg.domain.ResponseData
import utg.domain.UserId
import utg.domain.args.users.UpdateUserInput
import utg.domain.args.users.UpdateUserRole
import utg.domain.args.users.UserFilters
import utg.domain.args.users.UserInput
import utg.domain.auth.AccessCredentials
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.randomStr
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait UsersAlgebra[F[_]] {
  def get(filters: UserFilters): F[ResponseData[User]]
  def findById(id: UserId): F[Option[User]]
  def findByIds(ids: List[UserId]): F[Map[UserId, User]]
  def create(userInput: UserInput): F[UserId]
  def update(
      id: UserId,
      userInput: UpdateUserInput,
      fileMeta: Option[FileMeta],
    ): F[Unit]
  def updatePrivilege(userRole: UpdateUserRole): F[Unit]
}
object UsersAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID: Random](
      usersRepository: UsersRepository[F],
      assets: AssetsAlgebra[F],
      opersms: OperSmsClient[F],
    )(implicit
      P: PasswordHasher[F, SCrypt],
      logger: Logger[F],
    ): UsersAlgebra[F] =
    new UsersAlgebra[F] {
      override def get(filters: UserFilters): F[ResponseData[User]] =
        usersRepository.get(filters)

      override def findById(id: UserId): F[Option[User]] =
        usersRepository.findById(id)

      override def findByIds(ids: List[UserId]): F[Map[UserId, User]] =
        NonEmptyList.fromList(ids).fold(Map.empty[UserId, User].pure[F]) { userIds =>
          usersRepository.findByIds(userIds)
        }
      override def create(userInput: UserInput): F[UserId] =
        for {
          id <- ID.make[F, UserId]
          now <- Calendar[F].currentZonedDateTime
          user = dto.User(
            id = id,
            createdAt = now,
            firstname = userInput.firstname,
            lastname = userInput.lastname,
            roleId = userInput.roleId,
            login = userInput.login,
            phone = userInput.phone,
            assetId = None,
          )
          password <- randomStr[F](8)

          hash <- SCrypt.hashpw[F](password)

          accessCredentials = AccessCredentials(user, hash)
          _ <- usersRepository.create(accessCredentials)
          smsText =
            s"\n\nLogin: ${user.login}\nPassword: $password"
          _ <- opersms.send(userInput.phone, smsText, _ => Applicative[F].unit)
        } yield id

      override def update(
          id: UserId,
          userInput: UpdateUserInput,
          fileMeta: Option[FileMeta],
        ): F[Unit] =
        for {
          assetId <- fileMeta.traverse(assets.create)
          _ <- usersRepository.update(id)(
            _.copy(
              firstname = userInput.firstname,
              lastname = userInput.lastname,
              phone = userInput.phone,
              assetId = assetId,
            )
          )
        } yield {}

      override def updatePrivilege(userRole: UpdateUserRole): F[Unit] =
        usersRepository.update(userRole.userId)(
          _.copy(
            roleId = userRole.roleId
          )
        )
    }
}
