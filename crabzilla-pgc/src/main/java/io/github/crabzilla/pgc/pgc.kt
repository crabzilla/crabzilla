package io.github.crabzilla.pgc

import io.github.crabzilla.DomainEvent
import io.reactiverse.pgclient.PgTransaction
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler

typealias ProjectorHandler =
  (pgConn: PgTransaction, targetId: Int, event: DomainEvent, future: Handler<AsyncResult<Void>>) -> Unit

// extension function with helper for pgConn

fun PgTransaction.runPreparedQuery(query: String, tuple: Tuple, future: Future<Void>) {
  this.preparedQuery(query, tuple) { ar2 ->
    //    println("running $query with $tuple")
    if (ar2.failed()) {
//      println("    failed ${ar2.cause()}" )
      future.fail(ar2.cause())
    } else {
      future.complete()
    }
  }
}
