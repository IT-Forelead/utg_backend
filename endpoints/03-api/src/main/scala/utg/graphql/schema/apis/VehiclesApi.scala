package utg.graphql.schema.apis

import caliban.GraphQL
import caliban.RootResolver
import caliban.graphQL
import caliban.interop.cats.CatsInterop
import caliban.interop.cats.implicits._
import cats.MonadThrow

import utg.algebras.VehiclesAlgebra
import utg.domain.ResponseData
import utg.domain.Vehicle
import utg.domain.VehicleId
import utg.domain.args.vehicles.VehicleFilters
import utg.domain.args.vehicles.VehicleInput
import utg.domain.enums.Privilege
import utg.graphql.GraphQLContext
import utg.graphql.GraphQLTypes
import utg.graphql.schema.GraphQLApi
import utg.graphql.schema.Utils.Access

class VehiclesApi[F[_]: MonadThrow: Lambda[M[_] => CatsInterop[M, GraphQLContext]]](
    vehiclesAlgebra: VehiclesAlgebra[F]
  )(implicit
    ctx: GraphQLContext
  ) extends GraphQLTypes
       with GraphQLApi {
  import auto._

  private case class Mutations(
      @Access(Privilege.CreateUser) createVehicle: VehicleInput => F[VehicleId]
    )

  private case class Queries(
      @Access(Privilege.ViewUsers) vehicles: VehicleFilters => F[ResponseData[Vehicle]]
    )

  private val mutations: Mutations = Mutations(
    createVehicle = vehicleInput => vehiclesAlgebra.create(vehicleInput)
  )

  private val queries: Queries = Queries(
    vehicles = filter => vehiclesAlgebra.get(filter)
  )

  val api: GraphQL[GraphQLContext] = graphQL(RootResolver(queries, mutations))
}
