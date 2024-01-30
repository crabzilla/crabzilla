package io.github.crabzilla.handler

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.impl.PgPoolOptions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Handling concurrent commands")
class HandlingConcurrencyIT : AbstractCommandIT() {
  companion object {
    private val log = LoggerFactory.getLogger(HandlingConcurrencyIT::class.java)
  }

  @Test
  fun `when many concurrent RenameCustomer commands against same version, at least 1 will succeed`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val targetStream = TargetStream(stateType = "Customer", stateId = UUID.randomUUID().toString())
    val cmd = RegisterCustomer(UUID.fromString(targetStream.stateId), "good customer")
    crabzillaHandler.handle(targetStream, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        vertx.executeBlocking<Void> { promise ->
          val concurrencyLevel = PgPoolOptions.DEFAULT_MAX_SIZE + 200
          val executorService = Executors.newFixedThreadPool(concurrencyLevel)
          val cmd2 = CustomerCommand.RenameCustomer("new name")
          val callables = mutableSetOf<Callable<Future<EventMetadata>>>()
          for (i: Int in 1..concurrencyLevel) {
            callables.add(Callable { crabzillaHandler.handle(targetStream, cmd2) })
          }
          val futures = executorService.invokeAll(callables)
          executorService.awaitTermination(3, TimeUnit.SECONDS)
          val failures = futures.map { it.get() }.filter { it.failed() }
          val succeeded = futures.map { it.get() }.filter { it.succeeded() }
          log.info("Callables ${callables.size}, successes: ${succeeded.size}")
          tc.verify {
            assertEquals(futures.size, callables.size)
            assertThat(succeeded.size).isGreaterThan(0)
            for (f in failures) {
              log.info("${f.cause().javaClass.simpleName}, ${f.cause().message}")
              assertThat(f.cause().javaClass.simpleName).isIn("StreamCantBeLockedException", "PgException")
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

  @Test
  fun `when many concurrent ActivateCustomer commands against same version, just 1 will succeed`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerId = UUID.randomUUID()
    val targetStream = TargetStream(stateType = "Customer", stateId = customerId.toString())
    val cmd = RegisterCustomer(customerId, "good customer")
    crabzillaHandler.handle(targetStream, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        vertx.executeBlocking<Void> { promise ->
          val concurrencyLevel = PgPoolOptions.DEFAULT_MAX_SIZE + 100
          val executorService = Executors.newFixedThreadPool(concurrencyLevel)
          val cmd2 = ActivateCustomer("whatsoever")
          val callables = mutableSetOf<Callable<Future<EventMetadata>>>()
          for (i: Int in 1..concurrencyLevel) {
            callables.add(Callable { crabzillaHandler.handle(targetStream, cmd2) })
          }
          val futures = executorService.invokeAll(callables)
          executorService.awaitTermination(3, TimeUnit.SECONDS)
          val failures = futures.map { it.get() }.filter { it.failed() }
          val succeeded = futures.map { it.get() }.filter { it.succeeded() }
          log.info("Callables ${callables.size}, successes: ${succeeded.size}")
          tc.verify {
            assertEquals(futures.size, callables.size)
            assertThat(callables.size - failures.size).isCloseTo(1, Percentage.withPercentage(99.0))
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
