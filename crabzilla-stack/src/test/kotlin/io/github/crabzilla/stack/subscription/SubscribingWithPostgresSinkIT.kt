package io.github.crabzilla.stack.subscription

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.stack.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.github.crabzilla.stack.subscription.SubscriptionSink.POSTGRES_PROJECTOR
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class SubscribingWithPostgresSinkIT: AbstractSubscriptionIT() {

  override val subscriptionName = "crabzilla.example1.customer.SimpleProjector"

  companion object {
    private val id: UUID = UUID.randomUUID()
  }

  private val config =
    SubscriptionConfig(subscriptionName, initialInterval = 10, maxInterval = 100, sink = POSTGRES_PROJECTOR)

  @Test
  @Order(1)
  fun `it can project to postgres within an interval`(tc: VertxTestContext, vertx: Vertx) {
    val api = subsFactory.subscription(config, CustomersEventProjector())
    val options = CommandServiceOptions()
    val service = factory.commandService(customerComponent, jsonSerDer, options)
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

    api.deploy()
      .compose { service.handle(id, RegisterCustomer(id, "cust#$id")) }
      .compose { api.handle() }
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
    val service = factory.commandService(customerComponent, jsonSerDer)
    val config = SubscriptionConfig(subscriptionName, sink = POSTGRES_PROJECTOR)
    val api = subsFactory.subscription(config, CustomersEventProjector())
    api.deploy()
      .compose { service.handle(id, RegisterCustomer(id, "cust#$id")) }
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

  @Test
  @Order(3)
  fun `error scenario`(tc: VertxTestContext, vertx: Vertx) {
    val service = factory.commandService(customerComponent, jsonSerDer)
    val config = SubscriptionConfig(subscriptionName, sink = POSTGRES_PROJECTOR)
    val api = subsFactory.subscription(config, object: EventProjector {
      override fun project(conn: SqlConnection, eventRecord: EventRecord): Future<Void> {
        return Future.failedFuture("I am bad")
      }
    })
    api.deploy()
      .compose { service.handle(id, RegisterCustomer(id, "cust#$id")) }
      .compose { api.handle() }
      .compose {
        context.pgPool().preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() }
      }.onFailure {
        tc.failNow(it)
      }.onSuccess {
        tc.verify {
          assertEquals(0, it)
          tc.completeNow()
        }
      }
  }
}
