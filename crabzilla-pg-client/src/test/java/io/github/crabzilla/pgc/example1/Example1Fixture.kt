package io.github.crabzilla.pgc.example1

import io.github.crabzilla.core.CrabzillaContext
import io.github.crabzilla.core.CrabzillaInternal.EntityComponent
import io.github.crabzilla.core.SnapshotRepository
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.pgc.PgcSnapshotRepo
import io.github.crabzilla.pgc.PgcUowJournal
import io.github.crabzilla.pgc.PgcUowRepo
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import java.util.UUID
import kotlinx.serialization.json.Json

object Example1Fixture {

  const val CUSTOMER_ENTITY = "customer"

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

  val CUSTOMER_COMPONENT: (vertx: Vertx, writeDb: PgPool) -> EntityComponent<Customer> =
    { vertx: Vertx, writeDb: PgPool ->
      val cmdAware = CustomerCommandAware()
      val uowRepo = PgcUowRepo(writeDb, example1Json)
      val uowJournal = PgcUowJournal(vertx, writeDb, example1Json)
      val snapshotRepo:
        SnapshotRepository<Customer> = PgcSnapshotRepo(writeDb, example1Json, CustomerCommandAware())
      val ctx = CrabzillaContext(example1Json, uowRepo, uowJournal)
      EntityComponent(ctx, CUSTOMER_ENTITY, snapshotRepo, cmdAware)
  }
}
