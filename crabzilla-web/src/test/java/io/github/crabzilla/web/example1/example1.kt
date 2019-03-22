package io.github.crabzilla.web.example1

import io.github.crabzilla.example1.*
import io.github.crabzilla.pgclient.PgClientEventProjector
import io.github.crabzilla.pgclient.PgClientUowRepo
import io.github.crabzilla.pgclient.example1.EXAMPLE1_PROJECTOR_HANDLER
import io.github.crabzilla.vertx.CommandVerticle
import io.github.crabzilla.vertx.ProjectionData
import io.reactiverse.pgclient.PgPool
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Handler
import io.vertx.core.Vertx
import net.jodah.expiringmap.ExpiringMap
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("example1")

const val EXAMPLE1_PROJECTION_ENDPOINT: String = "example1_projection_endpoint"

fun customerCmdVerticle(vertx: Vertx, uowRepository: PgClientUowRepo): CommandVerticle<Customer> {

  val seedValue = Customer(null, null, false, null, PojoService())
  return CommandVerticle("Customer", seedValue, CUSTOMER_CMD_HANDLER, CUSTOMER_CMD_VALIDATOR,
    CUSTOMER_STATE_BUILDER, uowRepository, ExpiringMap.create(), CircuitBreaker.create("cb1", vertx))

}

fun setupEventHandler(vertx: Vertx, readDb: PgPool) {

  val eventProjector = PgClientEventProjector(readDb, "customer summary")

  vertx.eventBus().consumer<ProjectionData>(EXAMPLE1_PROJECTION_ENDPOINT) { message ->
    log.info("received events: " + message.body())
    eventProjector.handle(message.body(), EXAMPLE1_PROJECTOR_HANDLER, Handler { result ->
      if (result.failed()) {
        println("Projection failed: " + result.cause().message)
        return@Handler
      }
    })
  }

}
