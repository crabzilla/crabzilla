package io.github.crabzilla.usecases

import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.TestRepository
import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Handling Unit Of Work")
class HandlingUnitOfWorkIT {

  private lateinit var context: CrabzillaContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaContext.new(vertx, testDbConfig)
    testRepo = TestRepository(context.pgPool)
    cleanDatabase(context.pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can handle 2 commands within more than 1 instances of the same state`(vertx: Vertx, tc: VertxTestContext) {

    val options = FeatureOptions(pgNotificationInterval = 100L)
    val controller = context.commandController(customerComponent, jsonSerDer, options)

    val latch = CountDownLatch(2)
    val stateTypeMsg = AtomicReference<String>()
    val pgSubscriber = context.pgSubscriber()
    pgSubscriber.connect().onSuccess {
      pgSubscriber.channel(POSTGRES_NOTIFICATION_CHANNEL)
        .handler { stateType ->
          stateTypeMsg.set(stateType)
          latch.countDown()
        }
    }
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata.new(id)

    val id2 = UUID.randomUUID()
    val cmd2 = CustomerCommand.RegisterAndActivateCustomer(id2, "c2", "is needed")
    val metadata2 = CommandMetadata.new(id2)

    controller
      .compose { controller.handle(metadata, cmd)}
      .compose { controller.handle(metadata2, cmd2) }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        vertx.executeBlocking<Void> {
          tc.verify {
            latch.await(2, TimeUnit.SECONDS)
            assertThat(stateTypeMsg.get()).isEqualTo("Customer")
            it.complete()
          }
        }.onSuccess {
          tc.completeNow()
        }.onFailure {
          tc.failNow(it)
        }
      }
   }
}
