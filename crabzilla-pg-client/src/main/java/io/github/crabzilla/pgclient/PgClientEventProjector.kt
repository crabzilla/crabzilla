package io.github.crabzilla.pgclient

import io.github.crabzilla.DomainEvent
import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.PgPool
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory.getLogger

class PgClientEventProjector(private val pgPool: PgPool) {

  companion object {

    internal val log = getLogger(PgClientEventProjector::class.java)
    const val NUMBER_OF_FUTURES = 6 // CompositeFuture limit

  }

  fun handle(events: List<Pair<Int, DomainEvent>>,
             projectorHandler: (pgConn: PgConnection, targetId: Int, event: DomainEvent, future: Future<Void>) -> Unit,
             future: Future<Boolean>) {

    if (events.size > NUMBER_OF_FUTURES) {
      future.fail("only $NUMBER_OF_FUTURES events can be projected per transaction")
      return
    }

    pgPool.getConnection({ ar1 ->
      if (ar1.failed()) {
        log.error("getConn", ar1.cause())
        future.fail(ar1.cause())
        return@getConnection
      }
      val conn = ar1.result()

      // Begin the transaction
      val tx = conn
        .begin()
        .abortHandler({ _ ->
          run {
            log.error("Transaction failed = > rollbacked")
          }
        })

      val futures = futures(minOf(NUMBER_OF_FUTURES, events.size))

      for ((pairIndex, pair) in events.withIndex()) {
        // invoke the projection handler
        projectorHandler.invoke(conn, pair.first, pair.second, futures[pairIndex])
      }

      CompositeFuture.join(futures).setHandler { ar2 ->
        if (ar2.succeeded()) {
          // Commit the transaction
          tx.commit { ar3 ->
            if (ar3.succeeded()) {
              log.debug { "Transaction succeeded" }
              future.complete(true)
            } else {
              log.error("Transaction failed", ar3.cause())
              future.fail(ar3.cause())
            }
          }
        } else {
          log.error( "CompositeFuture error", ar2.cause())
          future.fail(ar2.cause())
        }
      }

    })

  }

  private fun futures(size: Int): List<Future<Void>> {
    val list = arrayListOf<Future<Void>>()
    for (i in 0 until size) {
      list.add(Future.future())
    }
    return list.toList()
  }

}
