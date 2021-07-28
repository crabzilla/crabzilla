package io.github.crabzilla.pgc

import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.pgc.command.CommandsContext
import io.github.crabzilla.stack.DomainStateId
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.deployVerticles
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
class AsyncProjectionIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(AsyncProjectionIT::class.java)
  }

  val id = UUID.randomUUID()

  lateinit var client: CommandsContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    client = CommandsContext.create(vertx, example1Json, connectOptions, poolOptions)
    testRepo = TestRepository(client.pgPool)
    val verticles = listOf(
      "service:crabzilla.example1.customer.CustomersEventsPublisher",
      "service:crabzilla.example1.customer.CustomersEventsProjector",
    )
    val options = DeploymentOptions().setConfig(config)
    cleanDatabase(client.sqlClient)
      .compose { vertx.deployVerticles(verticles, options) }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("closing db connections")
  fun cleanup(tc: VertxTestContext) {
    client.close()
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can create a command controller and send a command using default snapshot repository")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val snapshotRepo = SnapshotRepository<Customer>(client.pgPool, client.json)
    val controller = client.create(customerConfig)
    snapshotRepo.get(id)
      .compose { snapshot0: Snapshot<Customer>? ->
        assert(snapshot0 == null)
        controller.handle(CommandMetadata(DomainStateId(id)), RegisterCustomer(id, "cust#$id"))
      }.compose { s1: StatefulSession<Customer, CustomerEvent> ->
        snapshotRepo.get(id)
      }.compose { snapshot1 ->
        assert(1 == snapshot1!!.version)
        assert(Customer(id, "cust#$id") == snapshot1.state)
        controller.handle(
          CommandMetadata(DomainStateId(id)),
          ActivateCustomer("because yes")
        )
      }.compose { s2: StatefulSession<Customer, CustomerEvent> ->
        snapshotRepo.get(id)
      }.compose { snapshot2: Snapshot<Customer>? ->
        assert(2 == snapshot2!!.version)
        assert(
          Customer(
            id,
            "cust#$id",
            isActive = true,
            reason = "because yes"
          ) == snapshot2.state
        )
        Future.succeededFuture<Void>()
      }.compose {
        vertx.eventBus().request<Void>("crabzilla.publisher-customers", null)
      }.transform {
        if (it.failed()) {
          Future.failedFuture(it.cause())
        } else {
          Future.succeededFuture(testRepo.getProjections("customers"))
        }
      }.onFailure {
        tc.failNow(it)
      }.onSuccess {
        tc.completeNow()
      }
  }
}
