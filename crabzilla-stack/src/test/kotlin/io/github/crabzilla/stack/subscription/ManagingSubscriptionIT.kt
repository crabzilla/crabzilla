package io.github.crabzilla.stack.subscription

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.github.crabzilla.stack.command.internal.DefaultCommandServiceApi
import io.github.crabzilla.stack.subscription.SubscriptionSink.EVENTBUS_PUBLISH
import io.github.crabzilla.stack.subscription.SubscriptionSink.POSTGRES_PROJECTOR
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ManagingSubscriptionIT : AbstractSubscriptionIT() {

  override val subscriptionName = "crabzilla.example1.customer.SimpleProjector"

  companion object {
    private val id: UUID = UUID.randomUUID()
  }

  @Test
  @Order(1)
  fun `after deploy the status is intact`(tc: VertxTestContext, vertx: Vertx) {
    val config = SubscriptionConfig(subscriptionName, sink = EVENTBUS_PUBLISH)
    val api = subsFactory.subscription(config, CustomersEventProjector())
    api.deploy()
      .compose { api.status() }
      .onFailure { tc.failNow(it) }
      .onSuccess { json ->
        tc.verify {
          assertThat(api.name()).isEqualTo(subscriptionName)
          assertThat(api.isDeployed()).isTrue
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
    val config = SubscriptionConfig(subscriptionName, sink = EVENTBUS_PUBLISH)
    val api = subsFactory.subscription(config, CustomersEventProjector())
    val options = CommandServiceOptions()
    val service = factory.commandService(customerConfig, jsonSerDer, options)
    api.deploy()
      .compose { api.pause() }
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
  fun `after a command then work the currentOffset is 2`(tc: VertxTestContext, vertx: Vertx) {
    val config = SubscriptionConfig(subscriptionName, sink = EVENTBUS_PUBLISH)
    val api = subsFactory.subscription(config, CustomersEventProjector())
    val options = CommandServiceOptions()
    val service = factory.commandService(customerConfig, jsonSerDer, options)
    api.deploy()
      .compose {
        service.handle(id, RegisterCustomer(id, "cust#$id"))
      }.compose {
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
          assertEquals(2L, json.getLong("currentOffset"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(0L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(4)
  fun `after a command then work, the subscription is done`(tc: VertxTestContext, vertx: Vertx) {
    val config = SubscriptionConfig(subscriptionName, sink = POSTGRES_PROJECTOR)
    val api = subsFactory.subscription(config, CustomersEventProjector())
    val options = CommandServiceOptions()
    val service = factory.commandService(customerConfig, jsonSerDer, options)
    api.deploy()
      .compose {
        service.handle(id, RegisterCustomer(id, "cust#$id"))
      }.compose {
        api.handle()
      }.compose {
        context.pgPool().preparedQuery("select * from customer_summary").execute().map { rs -> rs.size() == 1 }
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
  val config = SubscriptionConfig(subscriptionName, sink = EVENTBUS_PUBLISH)
  val api = subsFactory.subscription(config, CustomersEventProjector())
  val options = CommandServiceOptions()
  val service = factory.commandService(customerConfig, jsonSerDer, options)
  api.deploy()
    .compose {  service.handle(id, RegisterCustomer(id, "cust#$id")) }
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
fun `after a command then pause then resume the paused is false and currentOffset is 2`(
  tc: VertxTestContext,
  vertx: Vertx
) {
  val config = SubscriptionConfig(subscriptionName, sink = EVENTBUS_PUBLISH)
  val api = subsFactory.subscription(config, CustomersEventProjector())
  val options = CommandServiceOptions()
  val service = DefaultCommandServiceApi(context, customerConfig, jsonSerDer, options)
  api.deploy()
    .compose {
      service.handle(id, RegisterCustomer(id, "cust#$id")) }
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
        assertEquals(2L, json.getLong("currentOffset"))
        //          assertEquals(true, json.getBoolean("greedy"))
        assertEquals(0L, json.getLong("failures"))
        assertEquals(0L, json.getLong("backOff"))
        tc.completeNow()
      }
    }
}
}
