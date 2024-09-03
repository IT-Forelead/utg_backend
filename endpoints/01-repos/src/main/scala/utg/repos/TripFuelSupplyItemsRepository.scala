package utg.repos

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import skunk._
import uz.scala.skunk.syntax.all._

import utg.domain.FuelTypeAndQuantity
import utg.domain.TripFuelSupplyId
import utg.domain.TripFuelSupplyItemId
import utg.effects.GenUUID
import utg.repos.sql.TripFuelSupplyItemsSql
import utg.repos.sql.dto
import utg.utils.ID

trait TripFuelSupplyItemsRepository[F[_]] {
  def create(
      tripFuelSupplyId: TripFuelSupplyId,
      fuels: NonEmptyList[FuelTypeAndQuantity],
    ): F[Unit]
  def getByTripFuelSupplyId(id: TripFuelSupplyId): F[List[dto.TripFuelSupplyItem]]
}

object TripFuelSupplyItemsRepository {
  def make[F[_]: MonadThrow: Async: GenUUID](
      implicit
      session: Resource[F, Session[F]]
    ): TripFuelSupplyItemsRepository[F] = new TripFuelSupplyItemsRepository[F] {
    override def create(
        tripFuelSupplyId: TripFuelSupplyId,
        fuels: NonEmptyList[FuelTypeAndQuantity],
      ): F[Unit] =
      fuels.traverse_ { item =>
        for {
          id <- ID.make[F, TripFuelSupplyItemId]
          dtoData = dto.TripFuelSupplyItem(
            id = id,
            tripFuelSupplyId = tripFuelSupplyId,
            fuelType = item.fuelType,
            fuelSupply = item.quantity,
          )
          _ <- TripFuelSupplyItemsSql.insert.execute(dtoData)
        } yield ()
      }

    override def getByTripFuelSupplyId(
        id: TripFuelSupplyId
      ): F[List[dto.TripFuelSupplyItem]] =
      TripFuelSupplyItemsSql.selectByTripFuelSupplyId.queryList(id)
  }
}
