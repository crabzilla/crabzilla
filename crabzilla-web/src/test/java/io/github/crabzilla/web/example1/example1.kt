package io.github.crabzilla.web.example1

import io.github.crabzilla.CommandHandlerVerticle
import io.github.crabzilla.ProjectionData
import io.github.crabzilla.SnapshotRepository
import io.github.crabzilla.example1.*
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcUowJournal
import io.github.crabzilla.pgc.example1.EXAMPLE1_PROJECTOR_HANDLER
import io.reactiverse.pgclient.PgPool
import io.vertx.core.Handler
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("example1")

const val EXAMPLE1_PROJECTION_ENDPOINT: String = "example1_projection_endpoint"

val EXAMPLE1_RESOURCE_TO_ENTITY = { resourceName: String ->
  when (resourceName) {
    "customers" -> "customer"
    else -> throw IllegalArgumentException("resource $resourceName is unknown")
  }
}

fun customerCmdVerticle(uowJournal: PgcUowJournal, snapshotRepo: SnapshotRepository<Customer>) :
  CommandHandlerVerticle<Customer> {
  return CommandHandlerVerticle("customer", CUSTOMER_CMD_FROM_JSON, CUSTOMER_SEED_VALUE, CUSTOMER_CMD_HANDLER_FACTORY,
    CUSTOMER_CMD_VALIDATOR, uowJournal, snapshotRepo)
}

fun setupEventHandler(vertx: Vertx, readDb: PgPool) {
  val eventProjector = PgcEventProjector(readDb, "customer summary")
  vertx.eventBus().consumer<ProjectionData>(EXAMPLE1_PROJECTION_ENDPOINT) { message ->
    log.info("received events: " + message.body())
    eventProjector.handle(message.body(), EXAMPLE1_PROJECTOR_HANDLER, Handler { result ->
      if (result.failed()) {
        log.error("Projection failed: " + result.cause().message)
      }
    })
  }
}
