package io.github.crabzilla.projection

import io.github.crabzilla.TestsFixtures
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomersPgEventProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.pgPool
import io.github.crabzilla.pgPoolOptions
import io.github.crabzilla.stack.CrabzillaConstants
import io.github.crabzilla.stack.command.CommandControllerOptions
import io.github.crabzilla.stack.command.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.pubsub.PgSubscriber
import jdk.jfr.Timespan.SECONDS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class ProjectingToEventsBusIT {

  companion object {
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
  fun `it can publish to eventbus as request reply to global topic`(tc: VertxTestContext, vertx: Vertx) {
    val config = CommandControllerOptions(pgNotificationInterval = 10L)
    val controller = CommandController.createAndStart(vertx, pgPool, TestsFixtures.json, customerConfig, config)
    val latch = CountDownLatch(2)
    val messages = mutableListOf<JsonObject>()
    vertx.eventBus().consumer<JsonObject>(CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC) { msg ->
      // crabzilla will send your events in order, but eventually it can repeat messages, so you must...
      // handle idempotency using the eventId (UUID)
      // "eventually" means when you use msg.fail() (maybe your broker is down) instead of msg.reply(null) (a success)
      // by using this feature, you can publish your events to wherever: Kafka, Pulsar, NATS, etc..
      messages.add(msg.body())
      msg.reply(null)
      latch.countDown()
    }
    val options = ProjectorConfig(
      projectionName, initialInterval = 10, interval = 10,
      projectorStrategy = ProjectorStrategy.EVENTBUS_REQUEST_REPLY,
      eventbusTopicStrategy = EventbusTopicStrategy.GLOBAL
    )
    val pgSubscriber = PgSubscriber.subscriber(vertx, pgPoolOptions)
    val component = EventsProjectorComponent(vertx, pgPool, pgSubscriber, options, CustomersPgEventProjector())
    component.start()
      .compose {
        controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
      }.compose {
        controller.handle(CommandMetadata.new(id), CustomerCommand.ActivateCustomer("because yes"))
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          assertTrue(latch.await(2, TimeUnit.SECONDS))
          val eventsTypes = messages.map { it.getJsonObject("eventPayload").getString("type") }
          assertEquals(listOf("CustomerRegistered", "CustomerActivated"), eventsTypes)
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(2)
  fun `it can publish to eventbus as request reply to stateType topic`(tc: VertxTestContext, vertx: Vertx) {
    val config = CommandControllerOptions(pgNotificationInterval = 10L)
    val controller = CommandController(vertx, pgPool, TestsFixtures.json, customerConfig, config).startPgNotification()
    val latch = CountDownLatch(1)
    val messages = mutableListOf<JsonObject>()
    vertx.eventBus().consumer<JsonObject>(CrabzillaConstants.stateTypeTopic("Customer")) { msg ->
      messages.add(msg.body())
      msg.reply(null)
      latch.countDown()
    }
    val options = ProjectorConfig(
      projectionName, initialInterval = 10, interval = 10,
      projectorStrategy = ProjectorStrategy.EVENTBUS_REQUEST_REPLY,
      eventbusTopicStrategy = EventbusTopicStrategy.STATE_TYPE
    )
    val pgSubscriber = PgSubscriber.subscriber(vertx, pgPoolOptions)
    val component = EventsProjectorComponent(vertx, pgPool, pgSubscriber, options, CustomersPgEventProjector())
    component.start()
      .compose {
        controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
      }.compose {
        vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          assertTrue(latch.await(2, TimeUnit.SECONDS))
          assertEquals(1, messages.size)
          assertEquals("Customer", messages[0].getString("stateType"))
        }
        tc.completeNow()
      }
  }
}
