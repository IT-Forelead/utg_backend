package utg.repos

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import eu.timepit.refined.types.string.NonEmptyString
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import utg.Phone
import utg.domain.AuthedUser.User
import utg.domain.RegionId
import utg.domain.ResponseData
import utg.domain.Role
import utg.domain.RoleId
import utg.domain.UserId
import utg.domain.args.users.UserFilters
import utg.domain.auth
import utg.domain.auth.AccessCredentials
import utg.domain.enums.Privilege
import utg.exception.AError
import utg.repos.sql.BranchesSql
import utg.repos.sql.RegionsSql
import utg.repos.sql.RolesSql
import utg.repos.sql.UsersSql
import utg.repos.sql.dto

trait UsersRepository[F[_]] {
  def find(phone: Phone): F[Option[AccessCredentials[User]]]
  def findById(id: UserId): F[Option[User]]
  def create(userAndHash: AccessCredentials[dto.User]): F[Unit]
  def update(id: UserId)(update: dto.User => dto.User): F[Unit]
  def changePassword(
      phone: Phone
    )(
      update: auth.AccessCredentials[dto.User] => auth.AccessCredentials[dto.User]
    ): F[Unit]
  def delete(id: UserId): F[Unit]
  def findByIds(ids: List[UserId]): F[Map[UserId, User]]
  def get(filters: UserFilters): F[ResponseData[User]]
  def getAsStream(filters: UserFilters): fs2.Stream[F, dto.User]
}

object UsersRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): UsersRepository[F] = new UsersRepository[F] {
    private def makeUser(userDto: dto.User): F[Option[User]] =
      for {
        optRole <- RolesSql.getById.queryList(userDto.roleId).map { privileges =>
          privileges.headOption.map { role =>
            Role(
              id = userDto.roleId,
              name = role.head,
              privileges = privileges.flatMap(_.tail.head),
            )
          }
        }
        branch <- userDto.branchCode.flatTraverse { branchCode =>
          (for {
            branch <- OptionT(BranchesSql.findByCode.queryOption(branchCode))
            region <- OptionT(RegionsSql.findById.queryOption(branch.regionId))
          } yield branch.toDomain(region.toDomain.some)).value
        }
        drivingLicenseCategories =
          userDto.drivingLicenseCategories.flatMap(NonEmptyList.fromList)
      } yield optRole.map { role =>
        userDto.toDomain(role, branch, drivingLicenseCategories)
      }

    private def makeUsers(dtos: List[dto.User]): F[List[User]] = {
      val roleIds = NonEmptyList.fromList(dtos.map(_.roleId))
      for {
        roles <- roleIds.fold(
          Map.empty[RoleId, NonEmptyString *: Option[Privilege] *: EmptyTuple].pure[F]
        ) { roleIds =>
          val rIds = roleIds.toList
          RolesSql
            .getByIds(rIds)
            .queryList(rIds)
            .map(_.map(r => r.head -> r.tail).toMap)
        }
        codes = NonEmptyList.fromList(dtos.flatMap(_.branchCode))
        branchByCode <- codes.fold(Map.empty[NonEmptyString, dto.Branch].pure[F]) { branches =>
          val branchesList = branches.toList
          BranchesSql
            .findByCodes(branchesList)
            .queryList(branchesList)
            .map(_.map(b => b.code -> b).toMap)
        }
        maybeRegionIds = NonEmptyList.fromList(branchByCode.values.toList.map(_.regionId))
        regionById <- maybeRegionIds.fold(Map.empty[RegionId, dto.Region].pure[F]) { regionIds =>
          val regionIdList = regionIds.toList
          RegionsSql
            .findByIds(regionIdList)
            .queryList(regionIdList)
            .map(a => a.map(r => r.id -> r).toMap)
        }
      } yield dtos.flatMap { userDto =>
        val roleOpt = roles.get(userDto.roleId)
        roleOpt.map { role =>
          val maybeBranch = userDto
            .branchCode
            .flatMap(branchByCode.get)
            .map(b => b.toDomain(regionById.get(b.regionId).map(_.toDomain)))
          val privelegies = roleOpt.flatMap(_.tail.head).toList
          val drivingLicenseCategories =
            userDto.drivingLicenseCategories.flatMap(NonEmptyList.fromList)
          userDto.toDomain(
            Role(
              id = userDto.roleId,
              name = role.head,
              privileges = privelegies,
            ),
            maybeBranch,
            drivingLicenseCategories,
          )
        }
      }
    }

    override def find(phone: Phone): F[Option[AccessCredentials[User]]] =
      OptionT(UsersSql.findByPhone.queryOption(phone)).flatMap { userData =>
        OptionT(makeUser(userData.data)).map(user => userData.copy(data = user))
      }.value

    override def findById(id: UserId): F[Option[User]] =
      OptionT(UsersSql.findById.queryOption(id)).flatMapF(makeUser).value

    override def create(userAndHash: AccessCredentials[dto.User]): F[Unit] =
      UsersSql.insert.execute(userAndHash)

    override def findByIds(ids: List[UserId]): F[Map[UserId, User]] =
      NonEmptyList.fromList(ids).fold(Map.empty[UserId, User].pure[F]) { userIds =>
        val uIds = userIds.toList
        UsersSql
          .findByIds(uIds)
          .queryList(uIds)
          .flatMap(makeUsers)
          .map(_.map(user => user.id -> user).toMap)
      }

    override def get(filters: UserFilters): F[ResponseData[User]] = {
      val af = UsersSql
        .select(filters)
        .paginateOpt(filters.limit.map(_.value), filters.page.map(_.value))
      af.fragment.query(UsersSql.codec *: int8).queryList(af.argument).flatMap { usersDto =>
        makeUsers(usersDto.map(_.head)).map { users =>
          ResponseData(users, usersDto.headOption.fold(0L)(_.tail.head))
        }
      }
    }

    override def update(id: UserId)(update: dto.User => dto.User): F[Unit] =
      OptionT(UsersSql.findById.queryOption(id)).cataF(
        AError.Internal(s"User not found by id [$id]").raiseError[F, Unit],
        user => UsersSql.update.execute(update(user)),
      )

    override def changePassword(
        phone: Phone
      )(
        update: auth.AccessCredentials[dto.User] => auth.AccessCredentials[dto.User]
      ): F[Unit] =
      OptionT(UsersSql.findByPhone.queryOption(phone)).cataF(
        AError.Internal(s"User not found by phone [$phone]").raiseError[F, Unit],
        user => UsersSql.changePassword.execute(update(user)),
      )

    override def delete(id: UserId): F[Unit] =
      UsersSql.delete.execute(id)

    override def getAsStream(filters: UserFilters): fs2.Stream[F, dto.User] = {
      val af =
        UsersSql.select(filters).paginateOpt(filters.limit.map(_.value), filters.page.map(_.value))
      af.fragment.query(UsersSql.codec *: int8).queryStream(af.argument).map(_._1)
    }
  }
}
