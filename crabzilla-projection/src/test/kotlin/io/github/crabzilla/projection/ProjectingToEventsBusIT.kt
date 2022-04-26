package io.github.crabzilla.projection

import io.github.crabzilla.TestsFixtures.json
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.pgConfig
import io.github.crabzilla.pgPool
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_REQUEST_REPLY
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class ProjectingToEventsBusIT {

  companion object {
    private val log = LoggerFactory.getLogger(ProjectingToEventsBusIT::class.java)
    const val projectionName = "crabzilla.example1.customer.SimpleProjector"
    private val projectorEndpoints = ProjectorEndpoints(projectionName)
    private val id: UUID = UUID.randomUUID()
  }

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
  fun `it can publish to eventbus using request reply`(tc: VertxTestContext, vertx: Vertx) {
    val factory = EventsProjectorFactory(pgPool, pgConfig)
    val config = ProjectorConfig(projectionName, initialInterval = 1, interval = 30_000,
      projectorStrategy = EVENTBUS_REQUEST_REPLY
    )
    val verticle = factory.createVerticle(config)
    val controller = CommandController(vertx, pgPool, json, customerConfig)
    val latch = CountDownLatch(1)
    val message = AtomicReference<JsonArray>()
    var firstMessage = false
    vertx.eventBus().consumer<JsonArray>(EVENTBUS_GLOBAL_TOPIC) { msg ->
       // crabzilla will send your events in order, but eventually it can repeat messages, so you must...
       // handle idempotency using the eventId (UUID)
       // "eventually" means when you use msg.fail() (maybe your broker is down) instead of msg.reply(null) (a success)
       // by using this feature, you can publish your events to wherever: Kafka, Pulsar, NATS, etc..
       log.info("Received {}", msg.body().encodePrettily())
       if (!firstMessage) {
         log.info("*** got first message")
         firstMessage = true
         msg.reply(-1L)
         return@consumer
       }
       latch.countDown()
       message.set(msg.body())
       val eventSequence: Long = message.get().last()
         .let { jo -> val json = jo as JsonObject; json.getLong("eventSequence") }
       msg.reply(eventSequence)
    }

    val pingMessage = JsonArray().add(JsonObject().put("ping", 1))
    vertx.eventBus().request<Void>(EVENTBUS_GLOBAL_TOPIC, pingMessage)
      .compose { vertx.deployVerticle(verticle, DeploymentOptions().setInstances(1)) }
      .compose { controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id")) }
      .compose { controller.handle(CommandMetadata.new(id), CustomerCommand.ActivateCustomer("because yes")) }
      .compose { controller.flushPendingPgNotifications() }
      .compose { vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null) }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          vertx.executeBlocking<Void> {
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            log.info("Received {}", message.get().encodePrettily())
            val events = message.get().map { jo ->
              val json = jo as JsonObject
              Pair(json.getJsonObject("eventPayload").getString("type"), json.getLong("eventSequence"))
            }
            assertEquals(listOf(Pair("CustomerRegistered", 1L), Pair("CustomerActivated", 2L)), events)
            pgPool
              .preparedQuery("select sequence from projections where name = $1")
              .execute(Tuple.of(projectionName))
              .map { row: RowSet<Row> ->
                assertTrue(row.size() == 1 && row.value().first().getLong("sequence") == 2L)
              }.onSuccess {
                tc.completeNow()
              }.onFailure {
                tc.failNow(it)
              }
          }
        }
      }
  }

  @Test
  @Order(2)
  fun `it can publish to eventbus using request reply with a BLOCKING consumer`(tc: VertxTestContext, vertx: Vertx) {
    val factory = EventsProjectorFactory(pgPool, pgConfig)
    val config = ProjectorConfig(projectionName, initialInterval = 1, interval = 30_000,
      projectorStrategy = EVENTBUS_REQUEST_REPLY
    )
    val verticle = factory.createVerticle(config)
    val controller = CommandController(vertx, pgPool, json, customerConfig)
    val latch = CountDownLatch(1)
    val message = AtomicReference<JsonArray>()
    var firstMessage = false
    vertx.eventBus().consumer<JsonArray>(EVENTBUS_GLOBAL_TOPIC) { msg ->
      vertx.executeBlocking<Void> { promise ->
        log.info("Received {}", msg.body().encodePrettily())
        if (!firstMessage) {
          log.info("*** got first message")
          firstMessage = true
          msg.reply(-1L)
          promise.complete()
          return@executeBlocking
        }
        latch.countDown()
        message.set(msg.body())
        val eventSequence: Long = message.get().last()
          .let { jo -> val json = jo as JsonObject; json.getLong("eventSequence") }
        msg.reply(eventSequence)
        promise.complete()
      }
    }

    val pingMessage = JsonArray().add(JsonObject().put("ping", 1))
    vertx.eventBus().request<Void>(EVENTBUS_GLOBAL_TOPIC, pingMessage)
      .compose { vertx.deployVerticle(verticle, DeploymentOptions().setInstances(1)) }
      .compose { controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id")) }
      .compose { controller.handle(CommandMetadata.new(id), CustomerCommand.ActivateCustomer("because yes")) }
      .compose { controller.flushPendingPgNotifications() }
      .compose { vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null) }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          vertx.executeBlocking<Void> {
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            log.info("Received {}", message.get().encodePrettily())
            val events = message.get().map { jo ->
              val json = jo as JsonObject
              Pair(json.getJsonObject("eventPayload").getString("type"), json.getLong("eventSequence"))
            }
            assertEquals(listOf(Pair("CustomerRegistered", 1L), Pair("CustomerActivated", 2L)), events)
            pgPool
              .preparedQuery("select sequence from projections where name = $1")
              .execute(Tuple.of(projectionName))
              .map { row: RowSet<Row> ->
                assertTrue(row.size() == 1 && row.value().first().getLong("sequence") == 2L)
              }.onSuccess {
                tc.completeNow()
              }.onFailure {
                tc.failNow(it)
              }
          }
        }
      }
  }

  @Test
  @Order(3)
  fun `it can publish to eventbus using BLOCKING request reply`(tc: VertxTestContext, vertx: Vertx) {
    val factory = EventsProjectorFactory(pgPool, pgConfig)
    val config = ProjectorConfig(
      projectionName, initialInterval = 1, interval = 30_000,
      projectorStrategy = ProjectorStrategy.EVENTBUS_REQUEST_REPLY_BLOCKING
    )
    val verticle = factory.createVerticle(config)
    val controller = CommandController(vertx, pgPool, json, customerConfig)
    val latch = CountDownLatch(1)
    val message = AtomicReference<JsonArray>()
    var firstMessage = false
    vertx.eventBus().consumer<JsonArray>(EVENTBUS_GLOBAL_TOPIC) { msg ->
      log.info("Received {}", msg.body().encodePrettily())
      if (!firstMessage) {
        log.info("*** got first message")
        firstMessage = true
        msg.reply(-1L)
        return@consumer
      }
      latch.countDown()
      message.set(msg.body())
      val eventSequence: Long = message.get().last()
        .let { jo -> val json = jo as JsonObject; json.getLong("eventSequence") }
      msg.reply(eventSequence)
    }

    val pingMessage = JsonArray().add(JsonObject().put("ping", 1))
    vertx.eventBus().request<Void>(EVENTBUS_GLOBAL_TOPIC, pingMessage)
      .compose { vertx.deployVerticle(verticle, DeploymentOptions().setInstances(1)) }
      .compose { controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id")) }
      .compose { controller.handle(CommandMetadata.new(id), CustomerCommand.ActivateCustomer("because yes")) }
      .compose { controller.flushPendingPgNotifications() }
      .compose { vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null) }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          vertx.executeBlocking<Void> {
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            log.info("Received {}", message.get().encodePrettily())
            val events = message.get().map { jo ->
              val json = jo as JsonObject
              Pair(json.getJsonObject("eventPayload").getString("type"), json.getLong("eventSequence"))
            }
            assertEquals(listOf(Pair("CustomerRegistered", 1L), Pair("CustomerActivated", 2L)), events)
          }
          pgPool
            .preparedQuery("select sequence from projections where name = $1")
            .execute(Tuple.of(projectionName))
            .map { row: RowSet<Row> ->
              assertTrue(row.size() == 1 && row.value().first().getLong("sequence") == 2L)
            }.onSuccess {
              tc.completeNow()
            }.onFailure {
              tc.failNow(it)
            }
        }
      }
  }

  @Test
  @Order(4)
  fun `it can publish to eventbus`(tc: VertxTestContext, vertx: Vertx) {
    val factory = EventsProjectorFactory(pgPool, pgConfig)
    val config = ProjectorConfig(
      projectionName, initialInterval = 1, interval = 30_000,
      projectorStrategy = ProjectorStrategy.EVENTBUS_PUBLISH
    )
    val verticle = factory.createVerticle(config)
    val controller = CommandController(vertx, pgPool, json, customerConfig)
    val latch = CountDownLatch(1)
    val message = AtomicReference<JsonArray>()
    var firstMessage = false
    vertx.eventBus().consumer<JsonArray>(EVENTBUS_GLOBAL_TOPIC) { msg ->
      log.info("Received {}", msg.body().encodePrettily())
      if (!firstMessage) {
        log.info("*** got first message")
        firstMessage = true
        msg.reply(-1L)
        return@consumer
      }
      latch.countDown()
      message.set(msg.body())
      val eventSequence: Long = message.get().last()
        .let { jo -> val json = jo as JsonObject; json.getLong("eventSequence") }
      msg.reply(eventSequence)
    }

    val pingMessage = JsonArray().add(JsonObject().put("ping", 1))
    vertx.eventBus().request<Void>(EVENTBUS_GLOBAL_TOPIC, pingMessage)
      .compose { vertx.deployVerticle(verticle, DeploymentOptions().setInstances(1)) }
      .compose { controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id")) }
      .compose { controller.handle(CommandMetadata.new(id), CustomerCommand.ActivateCustomer("because yes")) }
      .compose { controller.flushPendingPgNotifications() }
      .compose { vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null) }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          vertx.executeBlocking<Void> {
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            log.info("Received {}", message.get().encodePrettily())
            val events = message.get().map { jo ->
              val json = jo as JsonObject
              Pair(json.getJsonObject("eventPayload").getString("type"), json.getLong("eventSequence"))
            }
            assertEquals(listOf(Pair("CustomerRegistered", 1L), Pair("CustomerActivated", 2L)), events)
          }
          pgPool
            .preparedQuery("select sequence from projections where name = $1")
            .execute(Tuple.of(projectionName))
            .map { row: RowSet<Row> ->
              assertTrue(row.size() == 1 && row.value().first().getLong("sequence") == 2L)
            }.onSuccess {
              tc.completeNow()
            }.onFailure {
              tc.failNow(it)
            }
        }
      }
  }

}
