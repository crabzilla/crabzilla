package io.github.crabzilla.pgc.query

import io.github.crabzilla.core.command.DomainEvent
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple

interface PgcDomainEventProjector {

  fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void>

  fun executePreparedQuery(tx: Transaction, query: String, tuple: Tuple): Future<Void> {
    val promise = Promise.promise<Void>()
    tx.preparedQuery(query)
      .execute(tuple) { event ->
        if (event.failed()) {
          promise.fail(event.cause())
        } else {
          promise.complete()
        }
      }
    return promise.future()
  }
}
