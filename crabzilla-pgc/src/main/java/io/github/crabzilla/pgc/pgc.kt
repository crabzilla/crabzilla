package io.github.crabzilla.pgc

import io.reactiverse.pgclient.PgTransaction
import io.reactiverse.pgclient.Tuple
import io.vertx.core.Future

/**
 * https://dzone.com/articles/three-paradigms-of-asynchronous-programming-in-ver
 */
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
