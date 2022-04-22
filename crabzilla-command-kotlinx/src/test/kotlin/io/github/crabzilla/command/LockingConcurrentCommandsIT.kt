package io.github.crabzilla.command

import io.github.crabzilla.TestsFixtures.json
import io.github.crabzilla.TestsFixtures.pgPool
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.impl.PgPoolOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Locking concurrent commands")
class LockingConcurrentCommandsIT {

  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    commandController = CommandController(vertx, pgPool, json, customerConfig)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can pessimistically lock the target state id`(vertx: Vertx, tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata.new(id)
    commandController.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess { sideEffect ->
        vertx.executeBlocking<Void> { promise ->
          val concurrencyLevel = PgPoolOptions.DEFAULT_MAX_SIZE + 2
          val executorService = Executors.newFixedThreadPool(concurrencyLevel)
          val cmd2 = CustomerCommand.ActivateCustomer("whatsoever")
          val metadata2 = CommandMetadata(id, metadata.causationId, sideEffect.latestEventId(), UUID.randomUUID())
          val callables = mutableSetOf<Callable<Future<CommandSideEffect>>>()
          for (i: Int in 1..concurrencyLevel) {
            callables.add(Callable { commandController.handle(metadata2, cmd2) })
          }
          val futures = executorService.invokeAll(callables)
          executorService.awaitTermination(3, TimeUnit.SECONDS)
          val failures = futures.map { it.get() }.filter { it.failed() }
          val succeeded = futures.map { it.get() }.filter { it.succeeded() }
          tc.verify {
            assertEquals(futures.size, callables.size)
            assertEquals(failures.size, callables.size - 1)
            assertEquals(succeeded.size, 1)
            for (f in failures) {
              val acceptable = when (f.cause().javaClass.simpleName) {
                "LockingException" -> true
                "PgException" -> true
                else -> false
              }
              assertTrue(acceptable, "Exception unexpected ${f.cause().javaClass.simpleName}")
            }
            promise.complete(null)
          }.failing<Void> {
            promise.fail(it)
          }
          executorService.shutdown()
        }.onSuccess {
          tc.completeNow()
        }.onFailure {
          tc.failNow(it)
        }
      }
  }
}
