package io.github.crabzilla.pgclient.example1

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.example1.CustomerActivated
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerDeactivated
import io.github.crabzilla.pgclient.ProjectorHandler
import io.github.crabzilla.pgclient.runPreparedQuery
import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("example1")

val EXAMPLE1_PROJECTOR_HANDLER: ProjectorHandler = {

  pgConn: PgConnection, targetId: Int, event: DomainEvent, handler: Handler<AsyncResult<Void>> ->

    log.info("event {} ", event)

    // TODO how can I compose here (1 event -> n writes) ?
    val future: Future<Void> = Future.future<Void>()
    future.setHandler(handler)

    when (event) {
      is CustomerCreated -> {
        val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
        val tuple = Tuple.of(targetId, event.name, false)
        pgConn.runPreparedQuery(query, tuple, future)
      }
      is CustomerActivated -> {
        val query = "UPDATE customer_summary SET is_active = true WHERE id = $1"
        val tuple = Tuple.of(targetId)
        pgConn.runPreparedQuery(query, tuple, future)
      }
      is CustomerDeactivated -> {
        val query = "UPDATE customer_summary SET is_active = false WHERE id = $1"
        val tuple = Tuple.of(targetId)
        pgConn.runPreparedQuery(query, tuple, future)
      }
      else -> log.info("${event.javaClass.simpleName} does not have any event projector handler")
    }

    log.info("finished event {} ", event)
}
