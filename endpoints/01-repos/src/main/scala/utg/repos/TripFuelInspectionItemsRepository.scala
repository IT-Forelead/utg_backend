package utg.repos

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.FuelTypeAndQuantity
import utg.domain.TripFuelInspectionId
import utg.domain.TripFuelInspectionItemId
import utg.effects.GenUUID
import utg.repos.sql.TripFuelInspectionItemsSql
import utg.repos.sql.dto
import utg.utils.ID

trait TripFuelInspectionItemsRepository[F[_]] {
  def create(
      tripFuelInspectionId: TripFuelInspectionId,
      fuels: NonEmptyList[FuelTypeAndQuantity],
    ): F[Unit]
  def getByTripFuelInspectionId(id: TripFuelInspectionId): F[List[dto.TripFuelInspectionItem]]
}

object TripFuelInspectionItemsRepository {
  def make[F[_]: MonadThrow: Async: GenUUID](
      implicit
      session: Resource[F, Session[F]]
    ): TripFuelInspectionItemsRepository[F] = new TripFuelInspectionItemsRepository[F] {
    override def create(
        tripFuelInspectionId: TripFuelInspectionId,
        fuels: NonEmptyList[FuelTypeAndQuantity],
      ): F[Unit] =
      fuels.traverse_ { item =>
        for {
          id <- ID.make[F, TripFuelInspectionItemId]
          dtoData = dto.TripFuelInspectionItem(
            id = id,
            tripFuelInspectionId = tripFuelInspectionId,
            fuelType = item.fuelType,
            fuelInTank = item.quantity,
          )
          _ <- TripFuelInspectionItemsSql.insert.execute(dtoData)
        } yield ()
      }

    override def getByTripFuelInspectionId(id: TripFuelInspectionId): F[List[dto.TripFuelInspectionItem]] =
      TripFuelInspectionItemsSql.selectById.queryList(id)
  }
}
