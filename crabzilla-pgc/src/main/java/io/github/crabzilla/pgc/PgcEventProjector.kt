package io.github.crabzilla.pgc

import io.github.crabzilla.ProjectionData
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.logging.LoggerFactory.getLogger

class PgcEventProjector(private val pgPool: PgPool, val name: String) {

  companion object {

    internal val log = getLogger(PgcEventProjector::class.java)
    const val NUMBER_OF_FUTURES = 6 // CompositeFuture limit

  }

  // TODO http://www.lemnik.com/blog/2018/12/31/Suspend-extensions-for-Vert-x-Database/
  // TODO coroutines to compose
  fun handle(uowProjectionData: ProjectionData, projectorHandler: ProjectorHandler, handler: Handler<AsyncResult<Void>>) {

    if (uowProjectionData.events.size > NUMBER_OF_FUTURES) {
      handler.handle(Future.failedFuture("only $NUMBER_OF_FUTURES events can be projected per transaction"))
      return
    }

    pgPool.getConnection { ar1 ->

      if (ar1.failed()) {
        handler.handle(Future.failedFuture(ar1.cause()))
        return@getConnection
      }
      val conn = ar1.result()

      // Begin the transaction
      val tx = conn
        .begin()
        .abortHandler { _ ->
          run {
            log.error("Transaction failed = > rollback")
          }
        }

      val futures = listOfFutures(minOf(NUMBER_OF_FUTURES, uowProjectionData.events.size))

      for ((pairIndex, event) in uowProjectionData.events.withIndex()) {
        // invoke the projection handler
        projectorHandler.invoke(conn, uowProjectionData.targetId, event, futures[pairIndex])
      }

      CompositeFuture.join(futures).setHandler { ar2 ->

        if (ar2.failed()) {
          handler.handle(Future.failedFuture(ar2.cause()))
          return@setHandler
        }

        conn.preparedQuery("insert into projections (name, last_uow) values ($1, $2) on conflict (name) do update set last_uow = $2",
          Tuple.of(name, uowProjectionData.uowSequence)) { ar3 ->

            if (ar3.failed()) {
              handler.handle(Future.failedFuture(ar3.cause()))
              return@preparedQuery
            }

            // Commit the transaction
            tx.commit { ar4 ->
              if (ar4.succeeded()) {
                log.debug { "Transaction succeeded" }
                handler.handle(Future.succeededFuture())
              } else {
                log.error("Transaction failed", ar4.cause())
                handler.handle(Future.failedFuture(ar4.cause()))
              }
            }

          }

      }

    }

  }

  private fun listOfFutures(size: Int): List<Future<Void>> {
    val list = arrayListOf<Future<Void>>()
    for (i in 0 until size) {
      list.add(Future.future())
    }
    return list.toList()
  }

}

