package io.github.crabzilla

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerModule
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

object TestsFixtures {

  val json = Json { serializersModule = customerModule }

  val stateSerDer = PolymorphicSerializer(Customer::class)
  val eventSerDer = PolymorphicSerializer(CustomerEvent::class)
  val commandSerDer = PolymorphicSerializer(CustomerCommand::class)

  val pgPool: PgPool = PgPool.pool(PgConnectOptionsFactory.from(dbConfig), PoolOptions())
  val testRepo = TestRepository(pgPool = pgPool)
}
