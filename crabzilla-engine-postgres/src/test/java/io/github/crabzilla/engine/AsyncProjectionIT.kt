package io.github.crabzilla.engine

import io.github.crabzilla.core.serder.JsonSerDer
import io.github.crabzilla.core.serder.KotlinJsonSerDer
import io.github.crabzilla.engine.command.CommandsContext
import io.github.crabzilla.engine.command.SnapshotRepository
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.StateId
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.command.Snapshot
import io.github.crabzilla.stack.deployVerticles
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
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
  lateinit var jsonSerDer: JsonSerDer
  lateinit var commandsContext: CommandsContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    commandsContext = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
    testRepo = TestRepository(commandsContext.pgPool)
    val verticles = listOf(
      "service:crabzilla.example1.customer.CustomersEventsPublisher",
      "service:crabzilla.example1.customer.CustomersEventsProjector",
    )
    val options = DeploymentOptions().setConfig(config)
    cleanDatabase(commandsContext.sqlClient)
      .compose { vertx.deployVerticles(verticles, options) }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("closing db connections")
  fun cleanup(tc: VertxTestContext) {
    commandsContext.close()
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  // TODO test idempotency

  @Test
  @DisplayName("it can create a command controller and send a command using default snapshot repository")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val snapshotRepo = SnapshotTestRepository<Customer>(commandsContext.pgPool, example1Json)
    val controller = commandsContext.create(customerConfig, SnapshotRepository.SnapshotType.PERSISTENT)
    snapshotRepo.get(id)
      .compose { snapshot0: Snapshot<Customer>? ->
        assert(snapshot0 == null)
        controller.handle(CommandMetadata(StateId(id)), RegisterCustomer(id, "cust#$id"))
      }.compose {
        snapshotRepo.get(id)
      }.compose { snapshot1 ->
        assert(1 == snapshot1!!.version)
        assert(Customer(id, "cust#$id") == snapshot1.state)
        controller.handle(
          CommandMetadata(StateId(id)),
          ActivateCustomer("because yes")
        )
      }.compose {
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
        vertx.eventBus().request<Void>("crabzilla.publisher-projection.customers", null)
      }.compose {
        // projection.customers
        val eventMetadata = EventMetadata(
          "Customer", StateId(id), EventId(UUID.randomUUID()),
          CorrelationId(UUID.randomUUID()), CausationId(UUID.randomUUID()), 1L
        )
        val eventJson = jsonSerDer.toJson(CustomerEvent.CustomerRegistered(id, "cust#$id"))
        val eventRecord = EventRecord(eventMetadata, JsonObject(eventJson))
        vertx.eventBus().request<Void>("projection.customers", eventRecord.toJsonObject())
      }.transform {
        if (it.failed()) {
          Future.failedFuture(it.cause())
        } else {
          Future.succeededFuture(testRepo.getProjections("projection.customers"))
        }
      }.onFailure {
        tc.failNow(it)
      }.onSuccess {
        tc.completeNow()
      }
  }
}
