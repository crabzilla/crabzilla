package io.github.crabzilla.projection.projectors

import io.github.crabzilla.TestsFixtures
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.dbConfig
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.pgPool
import io.github.crabzilla.projection.ProjectorEndpoints
import io.github.crabzilla.projection.verticle.deployProjector
import io.github.crabzilla.stack.CommandControllerOptions
import io.github.crabzilla.stack.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class EagerProjectorIT {

  private val projectorEndpoints = ProjectorEndpoints("crabzilla.example1.customer.EagerProjector")
  private val id: UUID = UUID.randomUUID()
  private lateinit var controller: CommandController<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    val options = CommandControllerOptions(pgNotificationInterval = 1L)
    controller = CommandController(vertx, pgPool, TestsFixtures.json, customerConfig, options)
      .startPgNotification()
    cleanDatabase(pgPool)
      .compose {
        vertx.deployProjector(dbConfig, "service:crabzilla.example1.customer.EagerProjector")
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  // TODO test idempotency

  private fun statusMatches(
    status: JsonObject,
    paused: Boolean,
    greedy: (Boolean) -> Boolean,
    failures: (Long) -> Boolean,
    currentOffset: Long
  ): Boolean {
    return status.getBoolean("paused") == paused &&
      greedy.invoke(status.getBoolean("greedy")) &&
      failures.invoke(status.getLong("failures")) &&
      status.getLong("currentOffset") == currentOffset
  }

  @Test
  @Order(1)
  fun `after deploy the failures are bigger than 0`(tc: VertxTestContext, vertx: Vertx) {
    Thread.sleep(100)
    vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      .onFailure { tc.failNow(it) }
      .onSuccess { msg ->
        if (statusMatches(msg.body(), false, { true }, { failures: Long -> failures > 0 }, currentOffset = 0L)) {
          tc.completeNow()
        } else {
          tc.failNow("unexpected status ${msg.body().encodePrettily()}")
        }
      }
  }

  @Test
  @Order(3)
  fun `after a command then work the currentOffset is 1`(tc: VertxTestContext, vertx: Vertx) {
    Thread.sleep(100)
    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .onFailure { tc.failNow(it) }
      .compose {
        Thread.sleep(100)
        vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
      }
      .onFailure { tc.failNow(it) }
      .compose {
        Thread.sleep(100)
        vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        if (statusMatches(msg.body(), paused = false, { true }, { true }, currentOffset = 1L)) {
          tc.completeNow()
        } else {
          tc.failNow("unexpected status ${msg.body().encodePrettily()}")
        }
      }
  }

  @Test
  @Order(4)
  fun `after a command then work, the projection is done`(tc: VertxTestContext, vertx: Vertx) {
    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .compose { vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null) }
      .compose {
        pgPool.preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() == 1 }
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

  @Test
  @Order(6)
  fun `after a command then pause then resume the paused is false and currentOffset is 1`(
    tc: VertxTestContext,
    vertx: Vertx
  ) {
    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .onFailure { tc.failNow(it) }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.pause(), null)
      }
      .onFailure { tc.failNow(it) }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.resume(), null)
      }
      .onFailure { tc.failNow(it) }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
      }
      .onFailure { tc.failNow(it) }
      .compose {
        Thread.sleep(100)
        vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        if (statusMatches(msg.body(), paused = false, { true }, { true }, currentOffset = 1L)) {
          tc.completeNow()
        } else {
          tc.failNow("unexpected status ${msg.body().encodePrettily()}")
        }
      }
  }
}
