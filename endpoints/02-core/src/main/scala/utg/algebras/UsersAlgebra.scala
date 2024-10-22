package utg.algebras

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.std.Random
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt
import uz.scala.syntax.refined._

import utg.Phone
import utg.domain.AssetId
import utg.domain.AuthedUser.User
import utg.domain.FileMeta
import utg.domain.ResponseData
import utg.domain.UserId
import utg.domain.args.smsMessages.SmsMessageInput
import utg.domain.args.users._
import utg.domain.auth._
import utg.domain.enums.DeliveryStatus
import utg.effects.Calendar
import utg.effects.GenUUID
import utg.randomStr
import utg.repos.UserLicensePhotosRepository
import utg.repos.UsersRepository
import utg.repos.sql.dto
import utg.utils.ID

trait UsersAlgebra[F[_]] {
  def get(filters: UserFilters): F[ResponseData[User]]
  def getAsStream(filters: UserFilters): F[fs2.Stream[F, dto.User]]
  def findById(id: UserId): F[Option[User]]
  def findByIds(ids: List[UserId]): F[Map[UserId, User]]
  def create(input: UserInput): F[UserId]
  def update(
      id: UserId,
      input: UpdateUserInput,
      fileMeta: Option[FileMeta[F]] = None,
    ): F[Unit]
  def delete(id: UserId): F[Unit]
  def updatePrivilege(userRole: UpdateUserRole): F[Unit]
  def findUser(phone: Phone): F[Option[AccessCredentials[User]]]
  def updatePassword(id: UserId, password: NonEmptyString): F[Unit]
}

object UsersAlgebra {
  def make[F[_]: Calendar: GenUUID: Random](
      usersRepository: UsersRepository[F],
      userLicensePhotosRepository: UserLicensePhotosRepository[F],
      smsMessagesAlgebra: SmsMessagesAlgebra[F],
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
          usersRepository.findByIds(userIds)
        }

      override def create(input: UserInput): F[UserId] =
        for {
          id <- ID.make[F, UserId]
          now <- Calendar[F].currentZonedDateTime
          user = dto.User(
            id = id,
            createdAt = now,
            firstname = input.firstname,
            lastname = input.lastname,
            middleName = input.middleName,
            personalId = input.personalId,
            personalNumber = input.personalNumber,
            birthday = input.birthday,
            placeOfBirth = input.placeOfBirth,
            address = input.address,
            roleId = input.roleId,
            phone = input.phone,
            branchCode = Option(input.branchCode),
            drivingLicenseNumber = input.drivingLicenseNumber,
            drivingLicenseCategories = input.drivingLicenseCategories.map(_.toList),
            drivingLicenseGiven = input.drivingLicenseGiven,
            drivingLicenseExpire = input.drivingLicenseExpire,
            drivingLicenseIssuingAuthority = input.drivingLicenseIssuingAuthority,
            machineOperatorLicenseNumber = input.machineOperatorLicenseNumber,
            machineOperatorLicenseCategories = input.machineOperatorLicenseCategories.map(_.toList),
            machineOperatorLicenseGiven = input.machineOperatorLicenseGiven,
            machineOperatorLicenseExpire = input.machineOperatorLicenseExpire,
            machineOperatorLicenseIssuingAuthority = input.machineOperatorLicenseIssuingAuthority,
          )
          password <- randomStr[F](8)
          hash <- SCrypt.hashpw[F](password)
          accessCredentials = AccessCredentials(user, hash)
          _ <- usersRepository.create(accessCredentials)
          _ <- input.licensePhotoIds.traverse { photoIds =>
            userLicensePhotosRepository.create(id, photoIds)
          }
          smsText =
            s"Sizning telefon raqamingiz UTG platformasidan ro'yxatdan o'tkazildi.\n %%UTG_DOMAIN%%\n Parolingiz: $password"
          smsMessageInput = SmsMessageInput(input.phone, smsText, DeliveryStatus.Sent)
          _ <- smsMessagesAlgebra.create(smsMessageInput)
        } yield id

      override def update(
          id: UserId,
          input: UpdateUserInput,
          fileMeta: Option[FileMeta[F]],
        ): F[Unit] =
        for {
          _ <- usersRepository.update(id)(
            _.copy(
              firstname = input.firstname,
              lastname = input.lastname,
              middleName = input.middleName,
              personalId = input.personalId,
              birthday = input.birthday,
              placeOfBirth = input.placeOfBirth,
              address = input.address,
              personalNumber = input.personalNumber,
              phone = input.phone,
              branchCode = Option(input.branchCode),
              roleId = input.roleId,
              drivingLicenseNumber = input.drivingLicenseNumber,
              drivingLicenseCategories = input.drivingLicenseCategories.map(_.toList),
              drivingLicenseGiven = input.drivingLicenseGiven,
              drivingLicenseExpire = input.drivingLicenseExpire,
              drivingLicenseIssuingAuthority = input.drivingLicenseIssuingAuthority,
              machineOperatorLicenseNumber = input.machineOperatorLicenseNumber,
              machineOperatorLicenseCategories =
                input.machineOperatorLicenseCategories.map(_.toList),
              machineOperatorLicenseGiven = input.machineOperatorLicenseGiven,
              machineOperatorLicenseExpire = input.machineOperatorLicenseExpire,
              machineOperatorLicenseIssuingAuthority = input.machineOperatorLicenseIssuingAuthority,
            )
          )
          _ <- input.licensePhotoIds.traverse { assetIds =>
            updateLicensePhotos(id, assetIds)
          }
        } yield ()

      override def updatePrivilege(userRole: UpdateUserRole): F[Unit] =
        usersRepository.update(userRole.userId)(
          _.copy(
            roleId = userRole.roleId
          )
        )

      override def delete(id: UserId): F[Unit] =
        usersRepository.delete(id)

      override def getAsStream(filters: UserFilters): F[fs2.Stream[F, dto.User]] =
        F.pure {
          usersRepository.getAsStream(filters)
        }

      override def findUser(phone: Phone): F[Option[AccessCredentials[User]]] =
        usersRepository.find(phone)

      override def updatePassword(id: UserId, password: NonEmptyString): F[Unit] =
        for {
          hash <- SCrypt.hashpw[F](password)
          _ <- usersRepository.updatePassword(id, hash)
        } yield {}

      private def updateLicensePhotos(
          userId: UserId,
          assetIds: NonEmptyList[AssetId],
        ): F[Unit] =
        for {
          _ <- userLicensePhotosRepository.deleteByUserId(userId)
          _ <- assetIds.traverse_ { assetId =>
            userLicensePhotosRepository.create(userId, NonEmptyList.one(assetId))
          }
        } yield ()
    }
}
