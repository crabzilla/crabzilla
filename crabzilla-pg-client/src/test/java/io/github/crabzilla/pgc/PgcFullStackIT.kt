package io.github.crabzilla.pgc

import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.pgc.CustomerProjectorVerticle.Companion.topic
import io.github.crabzilla.stack.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory

@ExtendWith(VertxExtension::class)
@Disabled
class PgcFullStackIT {

  // https://dev.to/sip3/how-to-write-beautiful-unit-tests-in-vert-x-2kg7
  // https://dev.to/cherrychain/tdd-in-an-event-driven-application-2d6i

  companion object {
    private val log = LoggerFactory.getLogger(CustomerProjectorVerticle::class.java)
  }

  val id = (0..10_000).random()
  val verticle = CustomerVerticle(20_000) // TODO decrease this to work each 1 second or...

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        cleanDatabase(vertx, config)
          .onFailure {
            log.error("Cleaning db", it)
            tc.failNow(it)
          }
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

  @Test
  @DisplayName("it can create a command controller, send a command and have both write and read model side effects")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val controller = CommandControllerFactory.createPublishingTo(topic, customerConfig, verticle.writeDb)
    assertThat(controller).isNotNull
    controller.handle(CommandMetadata(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
      .onSuccess {
        // TODO make bellow line to work without timeout (actually it should be a request instead of publish or send)
        //  vertx.eventBus().publish(PgcPoolingProjectionVerticle.PUBLISHER_ENDPOINT, null)
        tc.completeNow()
      }
      .onFailure {
        tc.failNow(it)
      }
  }
}
