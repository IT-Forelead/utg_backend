package utg

import cats.effect.Async
import cats.effect.Resource
import skunk.Session

import utg.repos._

case class Repositories[F[_]](
    users: UsersRepository[F],
    assets: AssetsRepository[F],
    roles: RolesRepository[F],
    regions: RegionsRepository[F],
    branches: BranchesRepository[F],
    vehicles: VehiclesRepository[F],
  )
object Repositories {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): Repositories[F] =
    Repositories(
      users = UsersRepository.make[F],
      assets = AssetsRepository.make[F],
      roles = RolesRepository.make[F],
      regions = RegionsRepository.make[F],
      branches = BranchesRepository.make[F],
      vehicles = VehiclesRepository.make[F],
    )
}
