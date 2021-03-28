package io.github.crabzilla.pgc

import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.core.Either
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.CustomerReadModelProjectorVerticle
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
  private val topic = "example1"

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        readDb = readModelPgPool(vertx, config)
        cleanDatabase(vertx, config)
          .onSuccess {
            // val publisherVerticle =
            // PgcSubscriberPublisherVerticle(topic, pgSubscriber(vertx, config), pgcEventsScanner, publisherVerticle)
            val publisherVerticle =
              PgcPoolingPublisherVerticle(PgcEventsScanner(writeDb), EventBusEventsPublisher(topic, vertx.eventBus()))
            val projectorVerticle = CustomerReadModelProjectorVerticle(customerJson, CustomerRepository(readDb))
            vertx.deployVerticle(publisherVerticle)
              .compose { vertx.deployVerticle(projectorVerticle) }
              .onFailure { err ->
                tc.failNow(err)
              }
              .onSuccess {
                tc.completeNow()
              }
          }.onFailure { tc.failNow(it) }
      }
      .onFailure { tc.failNow(it.cause) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can create a command controller, send a command and have both write and read model side effects")
  // TODO break it into smaller steps/assertions: check both write and real models persistence after handling a command
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val controller = CommandControllerFactory.createPublishingTo(topic, customerConfig, writeDb)
    assertThat(controller).isNotNull
    controller.handle(CommandMetadata(1), CustomerCommand.RegisterCustomer(1, "cust#1"))
      .onFailure { tc.failNow(it.cause) }
      .onSuccess { ok: Either<List<String>, StatefulSession<Customer, CustomerEvent>> ->
        when (ok) {
          is Either.Left -> println(ok.value.toString())
          is Either.Right -> {
            println(ok.value)
            vertx.eventBus().request<Long>(PgcPoolingPublisherVerticle.PUBLISHER_ENDPOINT, 1) { resp ->
              if (resp.failed()) {
                tc.failNow(resp.cause())
              } else {
                tc.completeNow()
              }
            }
          }
        }
      }
  }
}
