package io.github.crabzilla.web.query

import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.pgc.query.PgcDomainEventProjector
import io.github.crabzilla.web.example1.CustomerActivated
import io.github.crabzilla.web.example1.CustomerCreated
import io.github.crabzilla.web.example1.CustomerDeactivated
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple

class CustomerSummaryEventProjector : PgcDomainEventProjector {

  override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void> {
    return when (event) {
      is CustomerCreated -> {
        val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
        val tuple = Tuple.of(targetId, event.name, false)
        executePreparedQuery(pgTx, query, tuple)
      }
      is CustomerActivated -> {
        val query = "UPDATE customer_summary SET is_active = true WHERE id = $1"
        val tuple = Tuple.of(targetId)
        executePreparedQuery(pgTx, query, tuple)
      }
      is CustomerDeactivated -> {
        val query = "UPDATE customer_summary SET is_active = false WHERE id = $1"
        val tuple = Tuple.of(targetId)
        executePreparedQuery(pgTx, query, tuple)
      }
      else -> {
        failedFuture("${event.javaClass.simpleName} does not have any event projector handler")
      }
    }
  }
}
