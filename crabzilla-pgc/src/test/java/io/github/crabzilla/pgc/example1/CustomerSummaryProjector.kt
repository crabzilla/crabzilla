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

class CustomerSummaryProjector : PgcEventProjector {

  private val log = LoggerFactory.getLogger(CustomerSummaryProjector::class.java.name)

  override fun handle(pgTx: PgTransaction, targetId: Int, event: DomainEvent): Future<Void> {

    val future = Future.future<Void>()

    when (event) {
      is CustomerCreated -> {
        val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
        val tuple = Tuple.of(targetId, event.name, false)
        pgTx.runPreparedQuery(query, tuple, future)
      }
      is CustomerActivated -> {
        val query = "UPDATE customer_summary SET is_active = true WHERE id = $1"
        val tuple = Tuple.of(targetId)
        pgTx.runPreparedQuery(query, tuple, future)
      }
      is CustomerDeactivated -> {
        val query = "UPDATE customer_summary SET is_active = false WHERE id = $1"
        val tuple = Tuple.of(targetId)
        pgTx.runPreparedQuery(query, tuple, future)
      }
      else -> {
        val error = "${event.javaClass.simpleName} does not have any event projector handler"
        log.info(error)
        future.complete()
      }
    }

    return future

  }

}
