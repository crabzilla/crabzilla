package io.github.crabzilla.stack.subscription

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.stack.CrabzillaContext.Companion.EVENTBUS_GLOBAL_TOPIC
import io.github.crabzilla.stack.CrabzillaVertxContext
import io.github.crabzilla.stack.command.internal.CommandService
import io.github.crabzilla.stack.subscription.EventBusStrategy.*
import io.github.crabzilla.stack.subscription.internal.SubscriptionEndpoints
import io.github.crabzilla.testDbConfig
import io.vertx.core.Future
import io.vertx.core.Promise
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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
internal class SubscribingWithEventBusSinkIT {

  companion object {
    private val log = LoggerFactory.getLogger(SubscribingWithEventBusSinkIT::class.java)
    const val subscriptionName = "crabzilla.example1.customer.SimpleProjector"
    private val subscriptionEndpoints = SubscriptionEndpoints(subscriptionName)
    private val id: UUID = UUID.randomUUID()
  }

  private lateinit var context : CrabzillaVertxContext

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaVertxContext.new(vertx, testDbConfig)
    cleanDatabase(context.pgPool())
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.completeNow()
      }
  }

  @Test
  fun `it can publish to eventbus using request reply`(tc: VertxTestContext, vertx: Vertx) {
    val config = SubscriptionConfig(subscriptionName, eventBusStrategy = EVENTBUS_REQUEST_REPLY, interval = 10_000)
    val subscriptionApi = context.subscription(config)
    val service = CommandService(vertx, context.pgPool(), customerComponent, jsonSerDer)
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
      .compose { context.deploy() }
      .compose { service.handle(id, CustomerCommand.RegisterCustomer(id, "cust#$id")) }
      .compose { service.handle(id, CustomerCommand.ActivateCustomer("because yes")) }
      .compose { subscriptionApi.handle() }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          vertx.executeBlocking<Void> {
            assertTrue(latch.await(1, TimeUnit.SECONDS))
            log.info("Received {}", message.get().encodePrettily())
            val events = message.get().map { jo ->
              val json = jo as JsonObject
              Pair(json.getJsonObject("eventPayload").getString("type"), json.getLong("eventSequence"))
            }
            assertEquals(listOf(Pair("CustomerRegistered", 1L), Pair("CustomerActivated", 2L)), events)
            it.complete()
          }.compose {
            checkOffset(1, 2L)
          }.onSuccess {
            tc.completeNow()
          }.onFailure {
            tc.failNow(it)
          }
        }
      }
  }

  @Test
  @Disabled // instead, use EVENTBUS_REQUEST_REPLY_BLOCKING
  fun `it can publish to eventbus using request reply with a BLOCKING consumer`(tc: VertxTestContext, vertx: Vertx) {
    val config = SubscriptionConfig(subscriptionName, initialInterval = 1, interval = 30_000,
      eventBusStrategy = EVENTBUS_REQUEST_REPLY
    )
    val subscriptionApi = context.subscription(config)
    val service = CommandService(vertx, context.pgPool(), customerComponent, jsonSerDer)
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
      .compose { context.deploy() }
      .compose { service.handle(id, CustomerCommand.RegisterCustomer(id, "cust#$id")) }
      .compose { service.handle(id, CustomerCommand.ActivateCustomer("because yes")) }
      .compose { subscriptionApi.handle() }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          vertx.executeBlocking<Void> {
            assertTrue(latch.await(1, TimeUnit.SECONDS))
            log.info("Received {}", message.get().encodePrettily())
            val events = message.get().map { jo ->
              val json = jo as JsonObject
              Pair(json.getJsonObject("eventPayload").getString("type"), json.getLong("eventSequence"))
            }
            assertEquals(listOf(Pair("CustomerRegistered", 1L), Pair("CustomerActivated", 2L)), events)
            it.complete()
          }.compose {
            checkOffset(1, 2L)
          }.onSuccess {
            tc.completeNow()
          }.onFailure {
            tc.failNow(it)
          }
        }
      }
  }

  @Test
  fun `it can publish to eventbus using BLOCKING request reply`(tc: VertxTestContext, vertx: Vertx) {
    val config = SubscriptionConfig(subscriptionName, eventBusStrategy = EVENTBUS_REQUEST_REPLY_BLOCKING, interval = 10_000)
    val subscriptionApi = context.subscription(config)
    val service = CommandService(vertx, context.pgPool(), customerComponent, jsonSerDer)
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
      Thread.sleep(500) // just to prove we can block
      latch.countDown()
      message.set(msg.body())
      val eventSequence: Long = message.get().last()
        .let { jo -> val json = jo as JsonObject; json.getLong("eventSequence") }
      msg.reply(eventSequence)
    }
    val pingMessage = JsonArray().add(JsonObject().put("ping", 1))
    vertx.eventBus().request<Void>(EVENTBUS_GLOBAL_TOPIC, pingMessage)
      .compose { context.deploy() }
      .compose { service.handle(id, CustomerCommand.RegisterCustomer(id, "cust#$id")) }
      .compose { service.handle(id, CustomerCommand.ActivateCustomer("because yes")) }
      .compose { subscriptionApi.handle() }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          vertx.executeBlocking<Void> {
            assertTrue(latch.await(1, TimeUnit.SECONDS))
            log.info("Received {}", message.get().encodePrettily())
            val events = message.get().map { jo ->
              val json = jo as JsonObject
              Pair(json.getJsonObject("eventPayload").getString("type"), json.getLong("eventSequence"))
            }
            assertEquals(listOf(Pair("CustomerRegistered", 1L), Pair("CustomerActivated", 2L)), events)
            it.complete()
          }.compose {
            checkOffset(1, 2L)
          }.onSuccess {
            tc.completeNow()
          }.onFailure {
            tc.failNow(it)
          }
        }
      }
  }

  @Test
  fun `it can publish to eventbus`(tc: VertxTestContext, vertx: Vertx) {
    val config = SubscriptionConfig(subscriptionName, eventBusStrategy = EVENTBUS_PUBLISH, interval = 10_000)
    val subscriptionApi = context.subscription(config)
    val service = CommandService(vertx, context.pgPool(), customerComponent, jsonSerDer)
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
      .compose { context.deploy() }
      .compose { service.handle(id, CustomerCommand.RegisterCustomer(id, "cust#$id")) }
      .compose { service.handle(id, CustomerCommand.ActivateCustomer("because yes")) }
      .compose { subscriptionApi.handle() }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          vertx.executeBlocking<Void> {
            assertTrue(latch.await(1, TimeUnit.SECONDS))
            log.info("Received {}", message.get().encodePrettily())
            val events = message.get().map { jo ->
              val json = jo as JsonObject
              Pair(json.getJsonObject("eventPayload").getString("type"), json.getLong("eventSequence"))
            }
            assertEquals(listOf(Pair("CustomerRegistered", 1L), Pair("CustomerActivated", 2L)), events)
            it.complete()
           }.compose {
            checkOffset(1, 2L)
           }.onSuccess {
             tc.completeNow()
          }.onFailure {
            tc.failNow(it)
          }
        }
      }
  }

  private fun checkOffset(size: Int, sequence: Long): Future<Void> {
    val promise = Promise.promise<Void>()
    context.pgPool()
      .preparedQuery("select sequence from subscriptions where name = $1")
      .execute(Tuple.of(subscriptionName))
      .onSuccess { row: RowSet<Row> ->
        log.info("offset: ${row.first().toJson().encodePrettily()}")
        if (row.size() == size && row.value().first().getLong("sequence") == sequence) {
          promise.complete()
        } else {
          promise.fail("unexpected size or sequence ${row.size()}, ${row.first().toJson()}")
        }
      }.onFailure {
        promise.fail(it)
      }
    return promise.future()
  }
}
