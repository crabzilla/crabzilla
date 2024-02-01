package io.github.crabzilla.handler

import io.github.crabzilla.context.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.example1.customer.CustomerCommand
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
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Notifying postgres")
class NotifyingPostgresIT : AbstractCrabzillaHandlerIT() {
  @Test
  fun `it can notify Postgres`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val latch = CountDownLatch(1)
    val stateTypeMsg = AtomicReference<String>()
    val pgSubscriber = context.newPgSubscriber()
    pgSubscriber.connect().onSuccess {
      pgSubscriber.channel(POSTGRES_NOTIFICATION_CHANNEL)
        .handler { stateType ->
          stateTypeMsg.set(stateType)
          latch.countDown()
        }
    }
    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd = CustomerCommand.RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    crabzillaHandler.handle(targetStream1, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        val callable =
          Callable {
            tc.verify {
              assertTrue(latch.await(4, TimeUnit.SECONDS))
              assertThat(stateTypeMsg.get()).isEqualTo("Customer")
            }
          }
        vertx.executeBlocking(callable)
          .onSuccess { tc.completeNow() }
          .onFailure { tc.failNow(it) }
      }
  }
}
