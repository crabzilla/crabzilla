package io.github.crabzilla.stack.subscription

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.stack.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.stack.CrabzillaVertxContext
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
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
  private lateinit var context: CrabzillaVertxContext
  private lateinit var subscriptionApi: SubscriptionApi

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaVertxContext.new(vertx, testDbConfig)
    val config = SubscriptionConfig(subscriptionName, initialInterval = 10, interval = 10, maxInterval = 100)
    subscriptionApi = context.subscription(config, CustomersEventProjector())
    cleanDatabase(context.pgPool())
      .compose { context.deploy() }
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @Order(1)
  fun `it can project to postgres within an interval`(tc: VertxTestContext, vertx: Vertx) {
    val options = CommandServiceOptions()
    val service = context.commandService(customerComponent, jsonSerDer, options)
    val latch = CountDownLatch(1)
    val stateTypeMsg = AtomicReference<String>()
    val pgSubscriber = context.pgSubscriber()
    pgSubscriber.connect().onSuccess {
      pgSubscriber.channel(POSTGRES_NOTIFICATION_CHANNEL)
        .handler { stateType ->
          stateTypeMsg.set(stateType)
          latch.countDown()
        }
    }

    service.handle(id, RegisterCustomer(id, "cust#$id"))
      .compose { subscriptionApi.handle() }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        vertx.executeBlocking<Void> {
          tc.verify {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertThat(stateTypeMsg.get()).isEqualTo("Customer")
            it.complete()
          }
        }.compose {
          context.pgPool().preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() }
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
    val service = context.commandService(customerComponent, jsonSerDer)
    val config = SubscriptionConfig(subscriptionName)
    val api = context.subscription(config, CustomersEventProjector())
    service.handle(id, RegisterCustomer(id, "cust#$id"))
      .compose { api.handle() }
      .compose {
        context.pgPool().preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() }
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
