package utg.algebras

import caliban.uploads.FileMeta
import cats.Applicative
import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.std.Random
import cats.implicits._
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt
import uz.scala.integration.sms.OperSmsClient
import uz.scala.syntax.refined._

import utg.domain.AuthedUser.User
import utg.domain.ResponseData
import utg.domain.UserId
import utg.domain.args.users._
import utg.domain.auth._
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.randomStr
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait UsersAlgebra[F[_]] {
  def get(filters: UserFilters): F[ResponseData[User]]
  def getAsStream(filters: UserFilters): F[fs2.Stream[F, dto.User]]
  def findById(id: UserId): F[Option[User]]
  def findByIds(ids: List[UserId]): F[Map[UserId, User]]
  def create(userInput: UserInput): F[UserId]
  def update(
      id: UserId,
      userInput: UpdateUserInput,
      fileMeta: Option[FileMeta] = None,
    ): F[Unit]
  def delete(id: UserId): F[Unit]
  def updatePrivilege(userRole: UpdateUserRole): F[Unit]
  def changePassword(changePassword: Credentials): F[Unit]
}
object UsersAlgebra {
  def make[F[_]: Calendar: GenUUID: Random](
      usersRepository: UsersRepository[F],
      assets: AssetsAlgebra[F],
      opersms: OperSmsClient[F],
    )(implicit
      F: MonadThrow[F],
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
          usersRepository.findByIds(userIds.toList)
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
            middleName = userInput.middleName,
            roleId = userInput.roleId,
            phone = userInput.phone,
            assetId = None,
            branchCode = Option(userInput.branchCode),
            licenseNumber = userInput.licenseNumber,
            drivingLicenseCategories = userInput.drivingLicenseCategories.map(_.toList),
          )
          password <- randomStr[F](8)
          hash <- SCrypt.hashpw[F](password)
          accessCredentials = AccessCredentials(user, hash)
          _ = println("====================================")
          _ <- usersRepository.create(accessCredentials)
//          smsText =
//            s"Sizning telefon raqamingiz UTG platformasidan ro'yxatdan o'tkazildi.\n %%UTG_DOMAIN%%\n Parolingiz: $password"
//          _ <- opersms.send(userInput.phone, smsText, _ => Applicative[F].unit)
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
              middleName = userInput.middleName,
              phone = userInput.phone,
              branchCode = userInput.branchCode,
              roleId = userInput.roleId,
              assetId = assetId,
              licenseNumber = userInput.licenseNumber,
              drivingLicenseCategories = userInput.drivingLicenseCategories.map(_.toList),
            )
          )
        } yield {}

      override def updatePrivilege(userRole: UpdateUserRole): F[Unit] =
        usersRepository.update(userRole.userId)(
          _.copy(
            roleId = userRole.roleId
          )
        )

      override def changePassword(changePassword: Credentials): F[Unit] =
        for {
          hash <- SCrypt.hashpw[F](changePassword.password)
          _ <- usersRepository.changePassword(changePassword.phone)(
            _.copy(
              password = hash
            )
          )
        } yield {}

      override def delete(id: UserId): F[Unit] =
        usersRepository.delete(id)

      override def getAsStream(filters: UserFilters): F[fs2.Stream[F, dto.User]] =
        F.pure {
          usersRepository.getAsStream(filters)
        }
    }
}
