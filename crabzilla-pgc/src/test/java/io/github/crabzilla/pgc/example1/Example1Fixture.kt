package io.github.crabzilla.pgc.example1

import io.github.crabzilla.*
import io.github.crabzilla.example1.*
import io.github.crabzilla.example1.aggregate.Customer
import io.github.crabzilla.example1.aggregate.CustomerCommandAware
import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.pgc.PgcCmdHandler
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import java.time.Instant
import java.util.*

object Example1Fixture {

  const val CUSTOMER_ENTITY = "customer"

  val customerId1 = CustomerId(1)

  val createCmd1 = CreateCustomer("customer1")
  val created1 = CustomerCreated(customerId1, "customer1")
  val createdUow1 = UnitOfWork(CUSTOMER_ENTITY, customerId1.value, UUID.randomUUID(),
    "create", createCmd1, 1, listOf(Pair("CustomerCreated", created1)))

  val activateCmd1 = ActivateCustomer("I want it")
  val activated1 = CustomerActivated("a good reason", Instant.now())
  val activatedUow1 = UnitOfWork(CUSTOMER_ENTITY, customerId1.value, UUID.randomUUID(),
    "activate", activateCmd1, 2, listOf(Pair("CustomerActivated", activated1)))

  val createActivateCmd1 = CreateActivateCustomer("customer1", "bcz I can")

  val deactivated1 = CustomerDeactivated("a good reason", Instant.now())

  val customerJson = CustomerJsonAware()

  val customerPgcComponent: (vertx: Vertx, writeDb: PgPool) -> EntityComponent<Customer> =
    { vertx: Vertx, writeDb: PgPool ->
      val jsonFunctions: Map<String, EntityJsonAware<out Entity>> = mapOf(Pair("customer", customerJson))
      PgcCmdHandler(writeDb, CUSTOMER_ENTITY, customerJson, CustomerCommandAware(),
        EventBusUowPublisher(vertx, jsonFunctions))
  }

}
