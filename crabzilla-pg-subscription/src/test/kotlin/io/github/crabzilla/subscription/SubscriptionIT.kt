package io.github.crabzilla.subscription

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.crabzilla.context.ViewTrigger
import io.github.crabzilla.example1.customer.effects.CustomersViewEffect
import io.github.crabzilla.example1.customer.effects.CustomersViewTrigger
import io.github.crabzilla.example1.customer.model.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterCustomer
import io.github.crabzilla.stream.TargetStream
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SubscriptionIT : AbstractSubscriptionTest() {
  fun api(
    viewTrigger: ViewTrigger? = null,
    subscriptionConfig: SubscriptionConfig = SubscriptionConfig(),
  ): SubscriptionApi {
    return SubscriptionComponentImpl(
      crabzillaContext = context,
      spec = SubscriptionSpec(SUBSCRIPTION_1),
      viewEffect = CustomersViewEffect(),
      viewTrigger = viewTrigger,
      config = subscriptionConfig,
    ).extractApi()
  }

  @Test
  @Order(1)
  fun `after deploy then status is intact`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val api = api()
    api.deploy()
      .compose { api.status() }
      .onFailure { tc.failNow(it) }
      .onSuccess { json ->
        tc.verify {
          assertThat(api.name()).isEqualTo(SUBSCRIPTION_1)
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
  fun `after a command when paused, then the currentOffset is 0`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val api = api()
    api.deploy()
      .compose { api.pause() }
      .compose {
        val targetStream1 = TargetStream(stateType = "Customer", stateId = CUSTOMER_ID.toString())
        commandHandler.handle(targetStream1, RegisterCustomer(CUSTOMER_ID, "cust#$CUSTOMER_ID"))
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
  fun `after a command when working then currentOffset is 1`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val api = api()
    api.deploy()
      .compose {
        val targetStream1 = TargetStream(stateType = "Customer", stateId = CUSTOMER_ID.toString())
        commandHandler.handle(targetStream1, RegisterCustomer(CUSTOMER_ID, "cust#$CUSTOMER_ID"))
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
          assertEquals(1L, json.getLong("currentOffset"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(0L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }

  @Test
  @Order(4)
  fun `after a command when working, then subscription is done`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val api = api()
    api.deploy()
      .compose {
        val targetStream1 = TargetStream(stateType = "Customer", stateId = CUSTOMER_ID.toString())
        commandHandler.handle(targetStream1, RegisterCustomer(CUSTOMER_ID, "cust#$CUSTOMER_ID"))
      }.compose {
        api.handle()
      }.compose {
        testRepository.getCustomers()
      }.onFailure {
        tc.failNow(it)
      }.onSuccess {
        if (it.size == 1) { // there is just one customer projected
          tc.completeNow()
        } else {
          tc.failNow("Nothing projected")
        }
      }
  }

  @Test
  @Order(5)
  fun `after a command then pause then work then paused is true and currentOffset is 0`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val api = api()
    api.deploy()
      .compose {
        val targetStream1 = TargetStream(stateType = "Customer", stateId = CUSTOMER_ID.toString())
        commandHandler.handle(targetStream1, RegisterCustomer(CUSTOMER_ID, "cust#$CUSTOMER_ID"))
      }
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
  fun `after a command then pause then resume then paused is false and currentOffset is 2`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val api = api()
    api.deploy()
      .compose {
        val targetStream1 = TargetStream(stateType = "Customer", stateId = CUSTOMER_ID.toString())
        commandHandler.handle(targetStream1, RegisterCustomer(CUSTOMER_ID, "cust#$CUSTOMER_ID"))
      }
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

  @Test
  @Order(4)
  fun `when using a viewTrigger, after a command when working, then the tigger is executed`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    data class CustomerView(
      @JsonProperty("id") val id: UUID,
      @JsonProperty("name") val name: String,
      @JsonProperty("is_active") val isActive: Boolean,
    )

    var viewAsJson = JsonObject()
    var customerView: CustomerView? = null
    val latch = CountDownLatch(1)
    vertx.eventBus().consumer<JsonObject>(CustomersViewTrigger.EVENTBUS_ADDRESS) { msg ->
      viewAsJson = msg.body()
      customerView = viewAsJson.mapTo(CustomerView::class.java)
      println("**** triggered since this customer id not active anymore: " + msg.body().encodePrettily())
      latch.countDown()
    }

    val api = api(CustomersViewTrigger(vertx.eventBus()))
    api.deploy()
      .compose {
        val targetStream1 = TargetStream(stateType = "Customer", stateId = CUSTOMER_ID.toString())
        commandHandler.handle(targetStream1, RegisterCustomer(CUSTOMER_ID, "cust#$CUSTOMER_ID"))
          .compose { commandHandler.handle(targetStream1, ActivateCustomer("because yes")) }
          .compose { commandHandler.handle(targetStream1, DeactivateCustomer("because yes")) }
      }.compose {
        api.handle()
      }.compose {
        testRepository.getCustomers()
      }.onFailure {
        tc.failNow(it)
      }.onSuccess {
        if (it.size == 1) { // only 1 customer expected
          latch.await(2, TimeUnit.SECONDS)
          assertThat(viewAsJson).isEqualTo(it.first())
          assertThat(customerView).isEqualTo(CustomerView(CUSTOMER_ID, "cust#$CUSTOMER_ID", false))
          tc.completeNow()
        } else {
          tc.failNow("Nothing projected")
        }
      }
  }

  @Test
  @Order(5)
  fun `no new events`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val api = api(subscriptionConfig = SubscriptionConfig().copy(initialInterval = 1, interval = 5, maxInterval = 10))
    api.deploy()
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
          assertEquals(false, json.getBoolean("greedy"))
          assertEquals(0L, json.getLong("currentOffset"))
          assertEquals(0L, json.getLong("failures"))
          assertEquals(1L, json.getLong("backOff"))
          tc.completeNow()
        }
      }
  }
  companion object {
    private val CUSTOMER_ID: UUID = UUID.randomUUID()
  }
}
