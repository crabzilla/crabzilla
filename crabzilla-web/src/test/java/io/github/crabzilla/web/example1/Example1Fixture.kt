package io.github.crabzilla.web.example1

import io.github.crabzilla.core.command.UnitOfWork
import java.util.UUID
import kotlinx.serialization.json.Json

object Example1Fixture {

  const val CUSTOMER_ENTITY = "customer"
  const val CUSTOMER_SUMMARY_STREAM = "customers-summary"

  const val customerId1 = 1

  val createCmd1 = CreateCustomer("customer1")
  val created1 = CustomerCreated(customerId1, "customer1")
  val createdUow1 = UnitOfWork(CUSTOMER_ENTITY, customerId1, UUID.randomUUID(), createCmd1, 1, listOf(created1))

  val activateCmd1 = ActivateCustomer("I want it")
  val activated1 = CustomerActivated("a good reason")
  val activatedUow1 = UnitOfWork(CUSTOMER_ENTITY, customerId1, UUID.randomUUID(), activateCmd1, 2, listOf(activated1))

  val createActivateCmd1 = CreateActivateCustomer("customer1", "bcz I can")

  val deactivated1 = CustomerDeactivated("a good reason")

  val example1Json = Json(context = customerModule)
}
