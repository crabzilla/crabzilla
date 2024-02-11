package io.github.crabzilla.writer

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.github.crabzilla.GenericCodec
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.example1.customer.effects.CustomerStateEffect
import io.github.crabzilla.example1.customer.model.Customer
import io.github.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.model.customerCommandHandler
import io.github.crabzilla.example1.customer.model.customerEventHandler
import io.github.crabzilla.example1.customer.serder.CustomerCommandSerDer
import io.github.crabzilla.example1.customer.serder.CustomerEventSerDer
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Caching a state after handling a command")
class HandlingStateEffects : AbstractWriterApiIT() {
  @Test
  fun `with a stateEffect, it will be called once`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val cache: LoadingCache<UUID, Customer> =
      Caffeine.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build { key: UUID? ->
          null
        }

    val customerConfig =
      WriterConfig(
        initialState = Customer.Initial,
        eventHandler = customerEventHandler,
        commandHandler = customerCommandHandler,
        commandSerDer = CustomerCommandSerDer(),
        eventSerDer = CustomerEventSerDer(),
        stateEffect = CustomerStateEffect(cache),
      )

    val writer = WriterApiImpl(context, customerConfig)

    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    vertx.eventBus().registerDefaultCodec(Customer.Active::class.java, GenericCodec(Customer.Active::class.java))
    vertx.eventBus().registerDefaultCodec(Customer.Inactive::class.java, GenericCodec(Customer.Inactive::class.java))

    writer.handle(targetStream1, cmd1)
      .compose {
        writer.handle(targetStream1, cmd2)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          assertThat(cache.get(customerId1)).isEqualTo(Customer.Inactive(customerId1, cmd1.name, "it's not needed anymore"))
          tc.completeNow()
        }
      }
  }
}
