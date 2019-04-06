package io.github.crabzilla.web.example1

import io.github.crabzilla.CommandHandlerVerticle
import io.github.crabzilla.ProjectionData
import io.github.crabzilla.SnapshotRepository
import io.github.crabzilla.example1.CUSTOMER_CMD_HANDLER
import io.github.crabzilla.example1.CUSTOMER_CMD_VALIDATOR
import io.github.crabzilla.example1.CUSTOMER_SEED_VALUE
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.pgclient.PgClientEventProjector
import io.github.crabzilla.pgclient.PgClientUowRepo
import io.github.crabzilla.pgclient.example1.EXAMPLE1_PROJECTOR_HANDLER
import io.reactiverse.pgclient.PgPool
import io.vertx.core.Handler
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("example1")

const val EXAMPLE1_PROJECTION_ENDPOINT: String = "example1_projection_endpoint"

fun customerCmdVerticle(uowRepository: PgClientUowRepo, snapshotRepo: SnapshotRepository<Customer>) :
  CommandHandlerVerticle<Customer> {
  return CommandHandlerVerticle("Customer", CUSTOMER_SEED_VALUE, CUSTOMER_CMD_HANDLER, CUSTOMER_CMD_VALIDATOR,
    uowRepository, snapshotRepo)
}

fun setupEventHandler(vertx: Vertx, readDb: PgPool) {
  val eventProjector = PgClientEventProjector(readDb, "customer summary")
  vertx.eventBus().consumer<ProjectionData>(EXAMPLE1_PROJECTION_ENDPOINT) { message ->
    log.info("received events: " + message.body())
    eventProjector.handle(message.body(), EXAMPLE1_PROJECTOR_HANDLER, Handler { result ->
      if (result.failed()) {
        log.error("Projection failed: " + result.cause().message)
        return@Handler
      }
    })
  }
}
