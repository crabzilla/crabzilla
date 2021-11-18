package io.github.crabzilla.projection

import io.github.crabzilla.command.CommandsContext
import io.github.crabzilla.command.SnapshotType
import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
class AsyncProjection2IT {

  private val id: UUID = UUID.randomUUID()
  lateinit var jsonSerDer: JsonSerDer
  lateinit var commandsContext: CommandsContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    commandsContext = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
    testRepo = TestRepository(commandsContext.pgPool)
    val newConfig = config
    newConfig.put("connectOptionsName", "ex1_crabzilla-config")
    newConfig.put("projectionName", "customers")
    newConfig.put("jsonFactoryClassName", "io.github.crabzilla.example1.Example1JsonContextFactory")
    newConfig.put("eventsProjectorFactoryClassName", "io.github.crabzilla.example1.customer.CustomersProjectorFactory")
    newConfig.put("stateTypes", JsonArray(listOf("Customer")))
    newConfig.put("eventTypes", JsonArray(listOf("CustomerRegistered")))
    val options = DeploymentOptions().setConfig(newConfig)
    cleanDatabase(commandsContext.sqlClient)
      .compose { vertx.deployVerticle(EventsProjectorVerticle(), options) }
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
  @DisplayName("after a command the events will be projected")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val controller = commandsContext.create(customerConfig, SnapshotType.ON_DEMAND)
    controller.handle(CommandMetadata(StateId(id)), RegisterCustomer(id, "cust#$id"))
      .compose {
        vertx.eventBus().request<Void>("crabzilla.projector.customers", null)
      }.compose {
        commandsContext.sqlClient.preparedQuery("select * from customer_summary")
          .execute()
          .map { rs ->
            rs.size() == 1
          }
      }.onFailure {
        tc.failNow(it)
      }.onSuccess {
        if (it) {
          tc.completeNow()
        } else {
          tc.failNow("Nothing projected")
        }
      }
  }
}
