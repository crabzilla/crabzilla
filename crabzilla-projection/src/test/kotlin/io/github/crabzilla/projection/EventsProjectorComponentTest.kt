package io.github.crabzilla.projection

import io.github.crabzilla.TestsFixtures
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomersPgEventProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.pgPool
import io.github.crabzilla.pgPoolOptions
import io.github.crabzilla.stack.CrabzillaConstants
import io.github.crabzilla.stack.command.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.pubsub.PgSubscriber
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
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class EventsProjectorComponentTest {

  companion object {
    const val projectionName = "crabzilla.example1.customer.EagerProjector"
  }

  private val projectorEndpoints = ProjectorEndpoints(projectionName)
  private val id: UUID = UUID.randomUUID()
  private lateinit var controller: CommandController<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    controller = CommandController(vertx, pgPool, TestsFixtures.json, customerConfig)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
  fun `it can publish to eventbus as request reply to global topic`(tc: VertxTestContext, vertx: Vertx) {
    val latch = CountDownLatch(1)
    val message = AtomicReference<JsonObject>()
    vertx.eventBus().consumer<JsonObject>(CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC) { msg ->
      message.set(msg.body())
      msg.reply(null)
      latch.countDown()
    }
    val options = ProjectorConfig(projectionName, projectorStrategy = ProjectorStrategy.EVENTBUS_REQUEST_REPLY)
    val pgSubscriber = PgSubscriber.subscriber(vertx, pgPoolOptions)
    val component = EventsProjectorComponent(vertx, pgPool, pgSubscriber, options, CustomersPgEventProjector())
    component.start()
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .compose {
            vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
          }
          .onFailure { tc.failNow(it) }
          .onComplete {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertEquals("Customer", message.get().getString("stateType"))
            tc.completeNow()
          }
      }
  }

  @Test
  @Order(2)
  fun `it can publish to eventbus as request reply to stateType topic`(tc: VertxTestContext, vertx: Vertx) {
    val latch = CountDownLatch(1)
    val message = AtomicReference<JsonObject>()
    vertx.eventBus().consumer<JsonObject>(CrabzillaConstants.stateTypeTopic("Customer")) { msg ->
      message.set(msg.body())
      msg.reply(null)
      latch.countDown()
    }
    val options = ProjectorConfig(
      projectionName,
      projectorStrategy = ProjectorStrategy.EVENTBUS_REQUEST_REPLY,
      eventbusTopicStrategy = EventbusTopicStrategy.STATE_TYPE
    )
    val pgSubscriber = PgSubscriber.subscriber(vertx, pgPoolOptions)
    val component = EventsProjectorComponent(vertx, pgPool, pgSubscriber, options, CustomersPgEventProjector())
    component.start()
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(CommandMetadata.new(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .compose {
            vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
          }
          .onFailure { tc.failNow(it) }
          .onComplete {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertEquals("Customer", message.get().getString("stateType"))
            tc.completeNow()
          }
      }
  }
}
