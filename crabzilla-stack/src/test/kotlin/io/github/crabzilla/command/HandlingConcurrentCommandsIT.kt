package io.github.crabzilla.command

import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.TestRepository
import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.testDbConfig
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
@DisplayName("Handling concurrent commands")
class HandlingConcurrentCommandsIT {

  companion object {
    private val log = LoggerFactory.getLogger(HandlingConcurrentCommandsIT::class.java)
  }

  private lateinit var context : CrabzillaContext
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
  fun `when many concurrent commands with same causationId, just one will succeed`(vertx: Vertx, tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata.new(id)
    val controller = context.featureController(customerComponent, jsonSerDer)
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess { sideEffect ->
        vertx.executeBlocking<Void> { promise ->
          val concurrencyLevel = PgPoolOptions.DEFAULT_MAX_SIZE + 200
          val executorService = Executors.newFixedThreadPool(concurrencyLevel)
          val cmd2 = ActivateCustomer("whatsoever")
          val metadata2 = CommandMetadata.new(id, sideEffect.latestEventId())
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
            assertThat(callables.size - failures.size).isEqualTo(1)
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


  @Test
  fun `when many concurrent commands without causationId, more than 1 will succeed`(vertx: Vertx, tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata.new(id)
    val controller = context.featureController(customerComponent, jsonSerDer)
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        vertx.executeBlocking<Void> { promise ->
          val concurrencyLevel = PgPoolOptions.DEFAULT_MAX_SIZE + 10
          val executorService = Executors.newFixedThreadPool(concurrencyLevel)
          val cmd2 = ActivateCustomer("whatsoever")
          val metadata2 = CommandMetadata.new(id)
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
            assertThat(callables.size - failures.size).isGreaterThan(1)
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
