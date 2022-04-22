package io.github.crabzilla

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerModule
import io.github.crabzilla.stack.PgConnectOptionsFactory
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

object TestsFixtures {

  val json = Json { serializersModule = customerModule }

  val stateSerDer = PolymorphicSerializer(Customer::class)
  val eventSerDer = PolymorphicSerializer(CustomerEvent::class)
  val commandSerDer = PolymorphicSerializer(CustomerCommand::class)

  val pgConfig = PgConnectOptionsFactory.from(dbConfig)
  val pgPoolOptions = PgConnectOptionsFactory.from(pgConfig)
  val pgPool: PgPool = PgPool.pool(pgPoolOptions, PoolOptions())

  val testRepo = TestRepository(pgPool = pgPool)
}
