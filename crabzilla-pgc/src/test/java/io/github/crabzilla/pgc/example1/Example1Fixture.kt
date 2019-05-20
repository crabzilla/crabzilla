package io.github.crabzilla.pgc.example1

import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.example1.*
import io.github.crabzilla.example1.CustomerCommandEnum.ACTIVATE
import io.github.crabzilla.example1.CustomerCommandEnum.CREATE
import java.time.Instant
import java.util.*

object Example1Fixture {

  const val entityName = "customer"

  val customerId1 = CustomerId(1)

  val createCmd1 = CreateCustomer("customer1")
  val created1 = CustomerCreated(customerId1, "customer1")
  val createdUow1 = UnitOfWork(UUID.randomUUID(), entityName, customerId1.value, UUID.randomUUID(),
    CREATE.urlFriendly(), createCmd1, 1, listOf(Pair("CustomerCreated", created1)))

  val activateCmd1 = ActivateCustomer("I want it")
  val activated1 = CustomerActivated("a good reason", Instant.now())
  val activatedUow1 = UnitOfWork(UUID.randomUUID(), entityName, customerId1.value, UUID.randomUUID(),
    ACTIVATE.urlFriendly(), activateCmd1, 2, listOf(Pair("CustomerActivated", activated1)))

  val deactivated1 = CustomerDeactivated("a good reason", Instant.now())

  val customerJson = CustomerJson()


}
