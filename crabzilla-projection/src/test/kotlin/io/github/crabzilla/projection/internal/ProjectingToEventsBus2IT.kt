package io.github.crabzilla.projection.internal

import io.github.crabzilla.TestsFixtures.json
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.pgConfig
import io.github.crabzilla.pgPool
import io.github.crabzilla.projection.EventsProjectorFactory
import io.github.crabzilla.projection.ProjectorConfig
import io.github.crabzilla.projection.ProjectorEndpoints
import io.github.crabzilla.projection.ProjectorStrategy
import io.github.crabzilla.stack.CrabzillaConstants
import io.github.crabzilla.stack.CrabzillaConstants.stateTypeTopic
import io.github.crabzilla.stack.command.CommandMetadata
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
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
internal class ProjectingToEventsBus2IT {

  companion object {
    private val log = LoggerFactory.getLogger(ProjectingToEventsBus2IT::class.java)
    const val projectionName = "crabzilla.example1.customer.SimpleProjector"
  }

  private val projectorEndpoints = ProjectorEndpoints(projectionName)
  private val id: UUID = UUID.randomUUID()

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
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
    vertx.eventBus().consumer<JsonArray>(CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC) { msg ->
      vertx.executeBlocking<Void> { promise ->
         // crabzilla will send your events in order, but eventually it can repeat messages, so you must...
         // handle idempotency using the eventId (UUID)
         // "eventually" means when you use msg.fail() (maybe your broker is down) instead of msg.reply(null) (a success)
         // by using this feature, you can publish your events to wherever: Kafka, Pulsar, NATS, etc..
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
         msg.reply(2) // TODO event sequence
         promise.complete()
      }
    }

    val pingMessage = JsonArray().add(JsonObject().put("ping", 1))
    vertx.eventBus().request<Void>(CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC, pingMessage)
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
//          val eventsTypes = messages.map { it.getJsonObject("eventPayload").getString("type") }
//          assertEquals(listOf("CustomerRegistered", "CustomerActivated"), eventsTypes)
          }
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(2)
  fun `it can publish to eventbus as request reply to stateType topic`(tc: VertxTestContext, vertx: Vertx) {
    val factory = EventsProjectorFactory(pgPool, pgConfig)
    val config = ProjectorConfig(
      projectionName, initialInterval = 1, interval = 1000,
      projectorStrategy = ProjectorStrategy.EVENTBUS_REQUEST_REPLY_BLOCKING,
    )
    val verticle = factory.createVerticle(config)
    val controller = CommandController(vertx, pgPool, json, customerConfig)
    val latch = CountDownLatch(1)
    val messages = mutableListOf<JsonObject>()
    vertx.eventBus().consumer<JsonObject>(stateTypeTopic("Customer")) { msg ->
      messages.add(msg.body())
      msg.reply(null)
      latch.countDown()
    }
    vertx.eventBus().request<Void>(stateTypeTopic("Customer"), JsonObject().put("field", "hi"))
    .compose { vertx.deployVerticle(verticle, DeploymentOptions().setInstances(1)) }
    .compose {  controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id")) }
    .onFailure { tc.failNow(it) }
    .onSuccess {
      Thread.sleep(2000)
      tc.verify {
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals(1, messages.size)
        assertEquals("Customer", messages[0].getString("stateType"))
      }
      tc.completeNow()
    }
  }
}
