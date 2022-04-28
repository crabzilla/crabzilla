package io.github.crabzilla.usecases

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.CommandSideEffect
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.pgPool
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.impl.PgPoolOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Locking concurrent commands")
class LockingConcurrentCommandsIT {

  companion object {
    private val log = LoggerFactory.getLogger(LockingConcurrentCommandsIT::class.java)
  }

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can pessimistically lock the target state id`(vertx: Vertx, tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata.new(id)
    val controller = CommandController(vertx, pgPool, customerComponent, jsonSerDer)
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess { sideEffect ->
        vertx.executeBlocking<Void> { promise ->
          val concurrencyLevel = PgPoolOptions.DEFAULT_MAX_SIZE + 100
          val executorService = Executors.newFixedThreadPool(concurrencyLevel)
          val cmd2 = CustomerCommand.ActivateCustomer("whatsoever")
          val metadata2 = CommandMetadata(id, metadata.causationId, sideEffect.latestEventId(), UUID.randomUUID())
          val callables = mutableSetOf<Callable<Future<CommandSideEffect>>>()
          for (i: Int in 1..concurrencyLevel) {
            callables.add(Callable { controller.handle(metadata2, cmd2) })
          }
          val futures = executorService.invokeAll(callables)
          executorService.awaitTermination(3, TimeUnit.SECONDS)
          val failures = futures.map { it.get() }.filter { it.failed() }
          val succeeded = futures.map { it.get() }.filter { it.succeeded() }
          log.info("Callables ${callables.size}, successes: ${succeeded.size}")
          tc.verify {
            assertEquals(futures.size, callables.size)
            assertThat(callables.size - failures.size).isIn(1, 2) // at most 2 successes
            for (f in failures) {
              log.info("${f.cause().javaClass.simpleName}, ${f.cause().message}")
              assertThat(f.cause().javaClass.simpleName).isIn("LockingException", "PgException")
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
