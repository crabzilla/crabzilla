package io.github.crabzilla.projection.verticle

import io.github.crabzilla.TestsFixtures
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.dbConfig
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.pgPool
import io.github.crabzilla.projection.ProjectorEndpoints
import io.github.crabzilla.stack.command.CommandControllerOptions
import io.github.crabzilla.stack.command.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@Disabled
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SimpleProjectorIT {

  private val projectorEndpoints = ProjectorEndpoints("crabzilla.example1.customer.SimpleProjector")
  private val id: UUID = UUID.randomUUID()

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .compose {
        vertx.deployProjector(dbConfig, "service:crabzilla.example1.customer.SimpleProjector")
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  // TODO test idempotency

  private fun statusMatches(
    status: JsonObject,
    paused: Boolean,
    greedy: Boolean,
    failures: Long,
    currentOffset: Long
  ): Boolean {
    return status.getBoolean("paused") == paused &&
      status.getBoolean("greedy") == greedy &&
      status.getLong("failures") == failures &&
      status.getLong("currentOffset") == currentOffset
  }

  @Test
  @Order(1)
  fun `after deploy the status is intact`(tc: VertxTestContext, vertx: Vertx) {
    vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      .onFailure { tc.failNow(it) }
      .onSuccess { msg ->
        if (statusMatches(msg.body(), paused = false, greedy = false, failures = 0L, currentOffset = 0L)) {
          tc.completeNow()
        } else {
          tc.failNow("unexpected status ${msg.body().encodePrettily()}")
        }
      }
  }

  @Test
  @Order(2)
  fun `after a command the greedy is true`(tc: VertxTestContext, vertx: Vertx) {
    val options = CommandControllerOptions(pgNotificationInterval = 1L)
    val controller = CommandController(vertx, pgPool, TestsFixtures.json, customerConfig, options)
    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .compose {
        controller.flushPendingPgNotifications()
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        val json = msg.body()
        tc.verify {
          assertTrue(json.getBoolean("greed"))
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(3)
  fun `after a command then work the currentOffset is 1`(tc: VertxTestContext, vertx: Vertx) {
    val options = CommandControllerOptions(pgNotificationInterval = 1L)
    val controller = CommandController(vertx, pgPool, TestsFixtures.json, customerConfig, options)
    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .compose {
        controller.flushPendingPgNotifications()
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        if (statusMatches(msg.body(), paused = false, greedy = true, failures = 0L, currentOffset = 1L)) {
          tc.completeNow()
        } else {
          tc.failNow("unexpected status ${msg.body().encodePrettily()}")
        }
      }
  }

  @Test
  @Order(4)
  fun `after a command then work, the projection is done`(tc: VertxTestContext, vertx: Vertx) {
    val options = CommandControllerOptions(pgNotificationInterval = 1L)
    val controller = CommandController(vertx, pgPool, TestsFixtures.json, customerConfig, options)
    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .compose {
        controller.flushPendingPgNotifications()
      }
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
}
