package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.pgc.PgcEventProjector
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple

class CustomerProjector : PgcEventProjector {

  override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void> {
    println("***** aqui capucaiana $event")
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
