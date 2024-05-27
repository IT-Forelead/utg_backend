package utg.repos

import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import utg.domain.Role
import utg.repos.sql.RolesSql

trait RolesRepository[F[_]] {
  def getAll: F[List[Role]]
}

object RolesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): RolesRepository[F] = new RolesRepository[F] {
    override def getAll: F[List[Role]] = for {
      dtoRoles <- RolesSql.getAll.queryList(Void)
      privileges <- RolesSql.getAllPrivileges.queryList(Void)
      pByRoleId = privileges.groupMap(_.head)(_.tail.head)
      roles = dtoRoles.map { role =>
        Role(
          id = role.id,
          name = role.name,
          privileges = pByRoleId.getOrElse(role.id, Nil),
        )
      }
    } yield roles
  }
}
