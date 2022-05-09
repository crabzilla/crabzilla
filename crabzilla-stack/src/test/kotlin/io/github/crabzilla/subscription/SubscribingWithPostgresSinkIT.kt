package io.github.crabzilla.subscription

import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
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
internal class SubscribingWithPostgresSinkIT {

  companion object {
    const val subscriptionName = "crabzilla.example1.customer.SimpleProjector"
  }

  private val id: UUID = UUID.randomUUID()
  private lateinit var context: CrabzillaContext

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaContext.new(vertx, testDbConfig)
    cleanDatabase(context.pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
  fun `it can project to postgres within an interval`(tc: VertxTestContext, vertx: Vertx) {
    val options = FeatureOptions(pgNotificationInterval = 100L)
    val controller = context.featureController(customerComponent, jsonSerDer, options)
    val config = SubscriptionConfig(subscriptionName, initialInterval = 10, interval = 100, maxInterval = 100)
    val (subscription, api) = context.subscriptionWithPostgresSink(config, CustomersEventProjector())

    val latch = CountDownLatch(1)
    val stateTypeMsg = AtomicReference<String>()
    val pgSubscriber = context.pgSubscriber()
    pgSubscriber.connect().onSuccess {
      pgSubscriber.channel(CrabzillaContext.POSTGRES_NOTIFICATION_CHANNEL)
        .handler { stateType ->
          stateTypeMsg.set(stateType)
          latch.countDown()
        }
    }

    vertx.deployVerticle(subscription)
      .compose { controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id")) }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        vertx.executeBlocking<Void> {
          tc.verify {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertThat(stateTypeMsg.get()).isEqualTo("Customer")
            it.complete()
          }
        }.compose {
          context.pgPool.preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() }
        }.onFailure {
          tc.failNow(it)
        }.onSuccess {
          tc.verify {
            assertEquals(1, it)
            tc.completeNow()
          }
        }
      }
  }

  @Test
  @Order(2)
  fun `it can project to postgres when explicit calling it`(tc: VertxTestContext, vertx: Vertx) {
    val controller = context.featureController(customerComponent, jsonSerDer)
    val config = SubscriptionConfig(subscriptionName)
    val (subscription, api) = context.subscriptionWithPostgresSink(config, CustomersEventProjector())
    vertx.deployVerticle(subscription)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(CommandMetadata.new(id), RegisterCustomer(id, "cust#$id"))
          .compose { api.handle() }
          .compose {
            context.pgPool.preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() }
          }.onFailure {
            tc.failNow(it)
          }.onSuccess {
            tc.verify {
              assertEquals(1, it)
              tc.completeNow()
            }
          }
      }
  }
}
