package io.github.crabzilla.pgc

import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.core.Either
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.CustomerReadModelProjector
import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerJson
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class CommandControllerFactoryTests {

  private lateinit var writeDb: PgPool
  private lateinit var readDb: PgPool

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        readDb = readModelPgPool(vertx, config)
        cleanDatabase(vertx, config)
      }
      .onFailure { tc.failNow(it.cause) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can create a command controller, send a command and have both write and read model side effects")
  // TODO break it into smaller steps/assertions: check both write and real models persistence after handling a command
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val controller = CommandControllerFactory.create(customerConfig, writeDb)
    assertThat(controller).isNotNull
    controller.handle(CommandMetadata(1), CustomerCommand.RegisterCustomer(1, "cust#1"))
      .onFailure { tc.failNow(it.cause) }
      .onSuccess { ok: Either<List<String>, StatefulSession<Customer, CustomerEvent>> ->
        when (ok) {
          is Either.Left -> println(ok.value.toString())
          is Either.Right -> {
            println(ok.value)
            val projector = CustomerReadModelProjector(CustomerRepository(readDb))
            val publisher = PgcEventsPublisher(projector, customerConfig.name.value, writeDb, customerJson)
            publisher.scan() // to force the scan of new events
              .onFailure { err ->
                err.printStackTrace()
                tc.failNow(err.cause)
              }
              .onSuccess {
                tc.completeNow()
                println("Cool! $it")
              }
          }
        }
      }
  }
}
