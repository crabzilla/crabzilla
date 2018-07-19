package io.github.crabzilla.pgclient

import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.Tuple
import io.vertx.core.Future

// extension function with helper for pgConn

fun PgConnection.pQuery(query: String, tuple: Tuple, future: Future<Void>) {
  this.preparedQuery(query, tuple, { ar2 ->
    if (ar2.failed()) {
      future.fail(ar2.cause())
    } else {
      future.complete()
    }
  })
}
