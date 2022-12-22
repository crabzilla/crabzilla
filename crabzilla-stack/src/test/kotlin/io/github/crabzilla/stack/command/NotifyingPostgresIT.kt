package io.github.crabzilla.stack.command

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.stack.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
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
@DisplayName("Notifying postgres")
class NotifyingPostgresIT: AbstractCommandIT() {

  @Test
  fun `it can notify Postgres`(vertx: Vertx, tc: VertxTestContext) {

    val options = CommandServiceOptions()
    val service = factory.commandService(customerConfig, jsonSerDer, options)

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
    val id = UUID.randomUUID().toString()
    val cmd = CustomerCommand.RegisterAndActivateCustomer(id, "c1", "is needed")
    service.handle(id, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        vertx.executeBlocking<Void> {
          tc.verify {
            assertTrue(latch.await(4, TimeUnit.SECONDS))
            assertThat(stateTypeMsg.get()).isEqualTo("Customer")
            it.complete()
          }
        }
        .onSuccess { tc.completeNow() }
        .onFailure { tc.failNow(it) }
      }
  }
}
