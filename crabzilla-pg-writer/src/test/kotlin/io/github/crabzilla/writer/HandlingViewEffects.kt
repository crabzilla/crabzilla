package io.github.crabzilla.writer

import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.example1.customer.effects.CustomersViewTrigger
import io.github.crabzilla.example1.customer.effects.CustomersViewTrigger.Companion.EVENTBUS_ADDRESS
import io.github.crabzilla.example1.customer.effects.CustomersWriteViewEffect
import io.github.crabzilla.example1.customer.model.Customer
import io.github.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.model.customerCommandHandler
import io.github.crabzilla.example1.customer.model.customerEventHandler
import io.github.crabzilla.example1.customer.serder.CustomerCommandSerDer
import io.github.crabzilla.example1.customer.serder.CustomerEventSerDer
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Projecting to view model synchronously")
class HandlingViewEffects : AbstractWriterApiIT() {
  @Test
  fun `without a viewEffect, customers table is empty`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerConfig =
      WriterConfig(
        initialState = Customer.Initial,
        eventHandler = customerEventHandler,
        commandHandler = customerCommandHandler,
        commandSerDer = CustomerCommandSerDer(),
        eventSerDer = CustomerEventSerDer(),
      )

    val writer = WriterApiImpl(context, customerConfig)

    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    writer.handle(targetStream1, cmd1)
      .compose {
        writer.handle(targetStream1, cmd2)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepository.getCustomers()
          .onFailure { tc.failNow(it) }
          .onSuccess { customersList ->
            tc.verify {
              assertThat(customersList.size).isEqualTo(0)
              tc.completeNow()
            }
          }
      }
  }

  @Test
  fun `with a viewEffect, customers table has 1 customer`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerConfig =
      WriterConfig(
        initialState = Customer.Initial,
        eventHandler = customerEventHandler,
        commandHandler = customerCommandHandler,
        commandSerDer = CustomerCommandSerDer(),
        eventSerDer = CustomerEventSerDer(),
        viewEffect = CustomersWriteViewEffect(),
      )

    val writer = WriterApiImpl(context, customerConfig)

    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    writer.handle(targetStream1, cmd1)
      .compose {
        writer.handle(targetStream1, cmd2)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepository.getCustomers()
          .onFailure { tc.failNow(it) }
          .onSuccess { customersList ->
            tc.verify {
              assertThat(customersList.size).isEqualTo(1)
              val json = customersList.first()
              assertThat(json.getString("id")).isEqualTo(targetStream1.stateId())
              assertThat(json.getString("name")).isEqualTo(cmd1.name)
              assertThat(json.getBoolean("is_active")).isEqualTo(false)
              tc.completeNow()
            }
          }
      }
  }

  @Test
  fun `with a viewTrigger checking for inactive customers, it will be called once`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerConfig =
      WriterConfig(
        initialState = Customer.Initial,
        eventHandler = customerEventHandler,
        commandHandler = customerCommandHandler,
        commandSerDer = CustomerCommandSerDer(),
        eventSerDer = CustomerEventSerDer(),
        viewEffect = CustomersWriteViewEffect(),
        viewTrigger = CustomersViewTrigger(vertx.eventBus()),
      )

    val writer = WriterApiImpl(context, customerConfig)

    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    val latch = CountDownLatch(1)
    val viewAsJson = AtomicReference<JsonObject?>(null)
    vertx.eventBus().consumer<JsonObject>(EVENTBUS_ADDRESS) { msg ->
      viewAsJson.set(msg.body())
      println("**** triggered since this customer id not active anymore: " + viewAsJson.get()!!.encodePrettily())
      latch.countDown()
    }

    writer.handle(targetStream1, cmd1)
      .compose {
        writer.handle(targetStream1, cmd2)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepository.getCustomers()
          .onFailure { tc.failNow(it) }
          .onSuccess { customersList ->
            tc.verify {
              assertThat(customersList.size).isEqualTo(1)
              val json = customersList.first()
              assertThat(json.getString("id")).isEqualTo(targetStream1.stateId())
              assertThat(json.getString("name")).isEqualTo(cmd1.name)
              assertThat(json.getBoolean("is_active")).isEqualTo(false)
              latch.await(2, TimeUnit.SECONDS)
              assertThat(viewAsJson.get()).isEqualTo(json)
              tc.completeNow()
            }
          }
      }
  }
}
