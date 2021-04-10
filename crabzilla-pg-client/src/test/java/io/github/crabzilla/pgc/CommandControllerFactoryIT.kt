package io.github.crabzilla.pgc

import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.pgc.CustomerProjectorVerticle.Companion.topic
import io.github.crabzilla.stack.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory

@ExtendWith(VertxExtension::class)
class CommandControllerFactoryIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(CustomerProjectorVerticle::class.java)
  }

  val id = (0..10_000).random()
  val verticle = CustomerVerticle(10_000)
  lateinit var writeDb: PgPool
  lateinit var readDb: PgPool

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        readDb = readModelPgPool(vertx, config)
        cleanDatabase(vertx, config)
          .onFailure {
            log.error("Cleaning db", it)
            tc.failNow(it)
          }
          .onSuccess {
            log.info("Success")
            vertx.deployVerticle(verticle)
              .onFailure { err ->
                tc.failNow(err)
              }
              .onSuccess {
                tc.completeNow()
              }
          }
      }
  }

  // https://github.com/smallrye/smallrye-reactive-utils/blob/8798a76943afba436634d3c55ca47c00e26d52ca/vertx-mutiny-clients/vertx-mutiny-core/src/test/java/io/vertx/mutiny/test/EventbusTest.java#L47
  @Test
  @DisplayName("it can create a command controller, send a command and have both write and read model side effects")
  // TODO break it into smaller steps/assertions: check both write and real models persistence after handling a command
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val controller = CommandControllerFactory.createPublishingTo(topic, customerConfig, verticle.writeDb)
    assertThat(controller).isNotNull
    controller.handle(CommandMetadata(id), CustomerCommand.RegisterCustomer(id, "cust#1"))
      .onFailure { tc.failNow(it) }
      .onSuccess { session1 ->
        println(session1.toSessionData())
//        controller.handle(CommandMetadata(1), CustomerCommand.ActivateCustomer("don't ask"))
//          .onFailure { tc.failNow(it) }
//          .onSuccess { session2 ->
//            println(session2.toSessionData())
//            vertx.eventBus().publish(PgcPoolingProjectionVerticle.PUBLISHER_ENDPOINT, 1)
            tc.completeNow()
//            tc.awaitCompletion(10, TimeUnit.SECONDS)
//            vertx.eventBus().request<Long>(PgcPoolingProjectionVerticle.PUBLISHER_ENDPOINT, 1) { resp ->
//              if (resp.failed()) {
//                tc.failNow(resp.cause())
//              } else {
//                println("Result ${resp.result()}")
//                tc.completeNow()
//              }
//            }
//          }
      }
  }
}
