package io.github.crabzilla.projection

import io.github.crabzilla.TestsFixtures
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.dbConfig
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.kotlinx.KotlinxJsonObjectSerDer
import io.github.crabzilla.pgPool
import io.github.crabzilla.stack.CommandController
import io.github.crabzilla.stack.CommandControllerOptions
import io.github.crabzilla.stack.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ManagingProjectorIT {

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

  @Test
  @Order(1)
  fun `after deploy the status is intact`(tc: VertxTestContext, vertx: Vertx) {
    vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        val json = msg.body()
        tc.verify {
          assertEquals(false, json.getBoolean("paused"))
          assertEquals(false, json.getBoolean("busy"))
          assertEquals(false, json.getBoolean("greedy"))
          assertEquals(0L, json.getLong("currentOffset"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(0L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(2)
  fun `after pause then a command`(tc: VertxTestContext, vertx: Vertx) {
    val options = CommandControllerOptions(pgNotificationInterval = 1000L)
    val jsonSerDer = KotlinxJsonObjectSerDer(TestsFixtures.json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, jsonSerDer, options).startPgNotification()

    vertx.eventBus().request<JsonObject>(projectorEndpoints.pause(), null)
      .compose {
        controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      }.compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        val json = msg.body()
        tc.verify {
          assertEquals(true, json.getBoolean("paused"))
          assertEquals(false, json.getBoolean("busy"))
//          assertEquals(true, json.getBoolean("greedy"))
          assertEquals(0L, json.getLong("currentOffset"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(0L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(3)
  fun `after a command then work the currentOffset is 1`(tc: VertxTestContext, vertx: Vertx) {
    val options = CommandControllerOptions(pgNotificationInterval = 1000L)
    val jsonSerDer = KotlinxJsonObjectSerDer(TestsFixtures.json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, jsonSerDer, options).startPgNotification()

    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        val json = msg.body()
        tc.verify {
          assertEquals(false, json.getBoolean("paused"))
          assertEquals(false, json.getBoolean("busy"))
//          assertEquals(true, json.getBoolean("greedy"))
          assertEquals(1L, json.getLong("currentOffset"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(0L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(4)
  fun `after a command then work, the projection is done`(tc: VertxTestContext, vertx: Vertx) {
    val options = CommandControllerOptions(pgNotificationInterval = 1000L)
    val jsonSerDer = KotlinxJsonObjectSerDer(TestsFixtures.json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, jsonSerDer, options).startPgNotification()

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
  @Order(5)
  fun `after a command then pause then work the paused is true and currentOffset is 0`(
    tc: VertxTestContext,
    vertx: Vertx
  ) {
    val options = CommandControllerOptions(pgNotificationInterval = 1000L)
    val jsonSerDer = KotlinxJsonObjectSerDer(TestsFixtures.json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, jsonSerDer, options).startPgNotification()

    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.pause(), null)
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        val json = msg.body()
        tc.verify {
          assertEquals(true, json.getBoolean("paused"))
          assertEquals(false, json.getBoolean("busy"))
//          assertEquals(true, json.getBoolean("greedy"))
          assertEquals(0L, json.getLong("currentOffset"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(0L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(6)
  fun `after a command then pause then resume the paused is false and currentOffset is 1`(
    tc: VertxTestContext,
    vertx: Vertx
  ) {
    val options = CommandControllerOptions(pgNotificationInterval = 1000L)
    val jsonSerDer = KotlinxJsonObjectSerDer(TestsFixtures.json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, jsonSerDer, options).startPgNotification()

    controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.pause(), null)
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.resume(), null)
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
      }
      .compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.status(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { msg: Message<JsonObject> ->
        val json = msg.body()
        tc.verify {
          assertEquals(false, json.getBoolean("paused"))
          assertEquals(false, json.getBoolean("busy"))
//          assertEquals(true, json.getBoolean("greedy"))
          assertEquals(1L, json.getLong("currentOffset"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(0L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }
}
