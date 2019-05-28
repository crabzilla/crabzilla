package io.github.crabzilla.pgc.example1

import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.example1.*
import io.github.crabzilla.example1.CustomerCommandEnum.ACTIVATE
import io.github.crabzilla.example1.CustomerCommandEnum.CREATE
import io.github.crabzilla.pgc.PgcEntityComponent
import io.reactiverse.pgclient.PgPool
import java.time.Instant
import java.util.*

object Example1Fixture {

  const val customerEntityName = "customer"

  val customerId1 = CustomerId(1)

  val createCmd1 = CreateCustomer("customer1")
  val created1 = CustomerCreated(customerId1, "customer1")
  val createdUow1 = UnitOfWork(customerEntityName, customerId1.value, UUID.randomUUID(),
    CREATE.urlFriendly(), createCmd1, 1, listOf(Pair("CustomerCreated", created1)))

  val activateCmd1 = ActivateCustomer("I want it")
  val activated1 = CustomerActivated("a good reason", Instant.now())
  val activatedUow1 = UnitOfWork(customerEntityName, customerId1.value, UUID.randomUUID(),
    ACTIVATE.urlFriendly(), activateCmd1, 2, listOf(Pair("CustomerActivated", activated1)))

  val createActivateCmd1 = CreateActivateCustomer("customer1", "bcz I can")

  val deactivated1 = CustomerDeactivated("a good reason", Instant.now())

  val customerJson = CustomerJson()

  val customerComponentFn: (writeDb: PgPool) -> PgcEntityComponent<Customer> = { pgPool ->
    PgcEntityComponent(customerEntityName, CustomerJson(), CustomerStateFn(), CustomerCmdFn(), pgPool)
  }

}
