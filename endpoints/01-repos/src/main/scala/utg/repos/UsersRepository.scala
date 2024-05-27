package utg.repos

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import utg.domain.{ResponseData, Role}
import utg.domain.auth.AccessCredentials
import utg.domain.args.users.UserFilters
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV
import utg.domain.AuthedUser.User
import utg.repos.sql.RolesSql
import utg.repos.sql.UsersSql
import utg.repos.sql.dto
import utg.EmailAddress
import utg.domain.UserId
import utg.exception.AError

trait UsersRepository[F[_]] {
  def find(email: EmailAddress): F[Option[AccessCredentials[User]]]
  def findById(id: UserId): F[Option[User]]
  def create(userAndHash: AccessCredentials[dto.User]): F[Unit]
  def update(id: UserId)(update: dto.User => dto.User): F[Unit]
  def findByIds(ids: NonEmptyList[UserId]): F[Map[UserId, User]]
  def get(filters: UserFilters): F[ResponseData[User]]
}

object UsersRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): UsersRepository[F] = new UsersRepository[F] {
    private def makeUser(userDto: dto.User): F[User] =
      RolesSql.getById.queryList(userDto.roleId).map { privileges =>
        userDto.toDomain(
          Role(
            id = userDto.roleId,
            name = privileges.head.head,
            privileges = privileges.map(_.tail.head),
          )
        )
      }

    private def makeUsers(dtos: List[dto.User]): F[List[User]] = {
      val roleIds = dtos.map(_.roleId)
      RolesSql
        .getByIds(roleIds)
        .queryList(roleIds)
        .map(_.groupMap(_.head)(_.tail))
        .map(roles =>
          dtos.map(userDto =>
            userDto.toDomain(
              Role(
                id = userDto.roleId,
                name = roles(userDto.roleId).head.head,
                privileges = roles(userDto.roleId).map(_.tail.head),
              )
            )
          )
        )
    }

    override def find(email: EmailAddress): F[Option[AccessCredentials[User]]] =
      OptionT(UsersSql.findByLogin.queryOption(email)).semiflatMap { userData =>
        makeUser(userData.data).map(user => userData.copy(data = user))
      }.value

    override def findById(id: UserId): F[Option[User]] =
      OptionT(UsersSql.findById.queryOption(id)).semiflatMap(makeUser).value

    override def create(userAndHash: AccessCredentials[dto.User]): F[Unit] =
      UsersSql.insert.execute(userAndHash)

    override def findByIds(ids: NonEmptyList[UserId]): F[Map[UserId, User]] = {
      val UserIds = ids.toList
      UsersSql
        .findByIds(UserIds)
        .queryList(UserIds)
        .flatMap(makeUsers)
        .map(_.map(user => user.id -> user).toMap)
    }
    override def get(filters: UserFilters): F[ResponseData[User]] = {
      val af = UsersSql
        .select(filters)
        .paginateOpt(filters.limit.map(_.value), filters.offset.map(_.value))
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
  }
}
