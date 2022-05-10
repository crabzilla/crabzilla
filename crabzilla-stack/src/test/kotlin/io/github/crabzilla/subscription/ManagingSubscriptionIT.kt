package io.github.crabzilla.subscription

import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.FeatureService
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.testDbConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
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
class ManagingSubscriptionIT {

  companion object {
    const val subscriptionName = "crabzilla.example1.customer.SimpleProjector"
    private val id: UUID = UUID.randomUUID()
  }

  private lateinit var context : CrabzillaContext
  private lateinit var api: SubscriptionApi

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaContext.new(vertx, testDbConfig)
    val config = SubscriptionConfig(subscriptionName)
    val pair: Pair<AbstractVerticle, SubscriptionApi> = context.subscriptionWithPostgresSink(config, CustomersEventProjector())
    api = pair.second
    cleanDatabase(context.pgPool)
      .compose { vertx.deployVerticle(pair.first) }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
  fun `after deploy the status is intact`(tc: VertxTestContext, vertx: Vertx) {
      api.status()
      .onFailure { tc.failNow(it) }
      .onSuccess { json ->
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
    val options = FeatureOptions(pgNotificationInterval = 100L)
    val service = FeatureService(vertx, context.pgPool, customerComponent, jsonSerDer, options)
      api.pause()
      .compose {
        service.handle(id, RegisterCustomer(id, "cust#$id"))
      }.compose {
        api.status()
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { json ->
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
    val options = FeatureOptions(pgNotificationInterval = 100L)
    val service = FeatureService(vertx, context.pgPool, customerComponent, jsonSerDer, options)
    service.handle(id, RegisterCustomer(id, "cust#$id"))
      .compose {
        api.handle()
      }.compose {
        api.status()
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { json ->
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
  fun `after a command then work, the subscription is done`(tc: VertxTestContext, vertx: Vertx) {
    val options = FeatureOptions(pgNotificationInterval = 1000L)
    val service = FeatureService(vertx, context.pgPool, customerComponent, jsonSerDer, options)
      service.handle(id, RegisterCustomer(id, "cust#$id"))
        .compose { api.handle()
        }.compose {
          context.pgPool.preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() == 1 }
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
    val options = FeatureOptions(pgNotificationInterval = 1000L)
    val service = FeatureService(vertx, context.pgPool, customerComponent, jsonSerDer, options)
      service.handle(id, RegisterCustomer(id, "cust#$id"))
      .compose {
        api.pause()
      }
      .compose {
        api.handle()
      }
      .compose {
        api.status()
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { json ->
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
    val options = FeatureOptions(pgNotificationInterval = 1000L)
    val service = FeatureService(vertx, context.pgPool, customerComponent, jsonSerDer, options)
    service.handle(id, RegisterCustomer(id, "cust#$id"))
      .compose {
        api.pause()
      }
      .compose {
        api.resume()
      }
      .compose {
        api.handle()
      }
      .compose {
        api.status()
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { json ->
        tc.verify {
          assertEquals(false, json.getBoolean("paused"))
          assertEquals(false, json.getBoolean("busy"))
          assertEquals(1L, json.getLong("currentOffset"))
          //          assertEquals(true, json.getBoolean("greedy"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(0L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }
}
