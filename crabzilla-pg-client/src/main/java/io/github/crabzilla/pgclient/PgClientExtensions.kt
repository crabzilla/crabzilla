package io.github.crabzilla.pgclient

import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.Tuple
import io.vertx.core.Future

// extension function with helper for pgConn

fun PgConnection.runPreparedQuery(query: String, tuple: Tuple, future: Future<Void>) {
  this.preparedQuery(query, tuple, { ar2 ->
//    println("running $query with $tuple")
    if (ar2.failed()) {
//      println("    failed ${ar2.cause()}" )
      future.fail(ar2.cause())
    } else {
      future.complete()
    }
  })
}
