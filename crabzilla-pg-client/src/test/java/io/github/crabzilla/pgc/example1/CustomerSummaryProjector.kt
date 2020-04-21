package io.github.crabzilla.pgc.example1

import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerDeactivated
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.runPreparedQuery
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple

class CustomerSummaryProjector : PgcEventProjector {
  override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void> {
    return when (event) {
      is CustomerCreated -> {
        val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
        val tuple = Tuple.of(targetId, event.name, false)
        pgTx.runPreparedQuery(query, tuple)
      }
      is CustomerActivated -> {
        val query = "UPDATE customer_summary SET is_active = true WHERE id = $1"
        val tuple = Tuple.of(targetId)
        pgTx.runPreparedQuery(query, tuple)
      }
      is CustomerDeactivated -> {
        val query = "UPDATE customer_summary SET is_active = false WHERE id = $1"
        val tuple = Tuple.of(targetId)
        pgTx.runPreparedQuery(query, tuple)
      }
      else -> {
        failedFuture("${event.javaClass.simpleName} does not have any event projector handler")
      }
    }
  }
}
