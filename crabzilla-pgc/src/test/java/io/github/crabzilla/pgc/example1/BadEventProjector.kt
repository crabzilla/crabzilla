package io.github.crabzilla.pgc.example1

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.example1.CustomerActivated
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerDeactivated
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.runPreparedQuery
import io.reactiverse.pgclient.PgTransaction
import io.reactiverse.pgclient.Tuple
import io.vertx.core.Future
import org.slf4j.LoggerFactory

class BadEventProjector : PgcEventProjector {

  private val log = LoggerFactory.getLogger(BadEventProjector::class.java.name)

  override fun handle(pgConn: PgTransaction, targetId: Int, event: DomainEvent): Future<Void> {

    log.info("event {} ", event)

    val future: Future<Void> = Future.future<Void>()

    when (event) {
      is CustomerCreated -> {
        val query = "INSERT INTO XXXXXX (id, name, is_active) VALUES ($1, $2, $3)"
        val tuple = Tuple.of(targetId, event.name, false)
        pgConn.runPreparedQuery(query, tuple, future)
      }
      is CustomerActivated -> {
        val query = "UPDATE XXX SET is_active = true WHERE id = $1"
        val tuple = Tuple.of(targetId)
        pgConn.runPreparedQuery(query, tuple, future)
      }
      is CustomerDeactivated -> {
        val query = "UPDATE XX SET is_active = false WHERE id = $1"
        val tuple = Tuple.of(targetId)
        pgConn.runPreparedQuery(query, tuple, future)
      }
      else -> {
        future.fail("${event.javaClass.simpleName} does not have any event projector handler")
        log.info("${event.javaClass.simpleName} does not have any event projector handler")
      }
    }

    log.info("finished event {} ", event)

    return future

  }

}
