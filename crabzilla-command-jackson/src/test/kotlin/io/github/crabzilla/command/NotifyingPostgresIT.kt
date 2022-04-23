package io.github.crabzilla.command

import io.github.crabzilla.Jackson.json
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.pgPool
import io.github.crabzilla.pgPoolOptions
import io.github.crabzilla.stack.CrabzillaConstants.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.stack.command.CommandControllerOptions
import io.github.crabzilla.stack.command.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.pubsub.PgSubscriber
import org.assertj.core.api.Assertions.assertThat
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
@DisplayName("Notifying postgres")
class NotifyingPostgresIT {

  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    val options = CommandControllerOptions(pgNotificationInterval = 100L)
    commandController = CommandController(vertx, pgPool, json, customerComponent, options)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can notify Postgres`(vertx: Vertx, tc: VertxTestContext) {
    val latch = CountDownLatch(1)
    val stateTypeMsg = AtomicReference<String>()
    val pgSubscriber = PgSubscriber.subscriber(vertx, pgPoolOptions)
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
    commandController.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        commandController.startPgNotification()
        tc.verify {
          latch.await(2, TimeUnit.SECONDS)
          assertThat(stateTypeMsg.get()).isEqualTo("Customer")
          tc.completeNow()
        }
      }
  }
}
