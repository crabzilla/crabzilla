package io.github.crabzilla.pgclient

import io.github.crabzilla.vertx.ProjectionData
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory.getLogger

class PgClientEventProjector(private val pgPool: PgPool, val name: String) {

  companion object {

    internal val log = getLogger(PgClientEventProjector::class.java)
    const val NUMBER_OF_FUTURES = 6 // CompositeFuture limit

  }

  // TODO http://www.lemnik.com/blog/2018/12/31/Suspend-extensions-for-Vert-x-Database/
  // TODO coroutines to compose
  fun handle(uowProjectionData: ProjectionData, projectorHandler: ProjectorHandler, future: Future<Boolean>) {

    if (uowProjectionData.events.size > NUMBER_OF_FUTURES) {
      future.fail("only $NUMBER_OF_FUTURES events can be projected per transaction")
      return
    }

    pgPool.getConnection { ar1 ->

      if (ar1.failed()) {
        future.fail(ar1.cause())
        return@getConnection
      }
      val conn = ar1.result()

      // Begin the transaction
      val tx = conn
        .begin()
        .abortHandler { _ ->
          run {
            log.error("Transaction failed = > rollbacked")
          }
        }

      val futures = listOfutures(minOf(NUMBER_OF_FUTURES, uowProjectionData.events.size))

      for ((pairIndex, event) in uowProjectionData.events.withIndex()) {
        // invoke the projection handler
        projectorHandler.invoke(conn, uowProjectionData.targetId, event, futures[pairIndex])
      }

      CompositeFuture.join(futures).setHandler { ar2 ->

        if (ar2.failed()) {
          log.error("*** here ", ar2.cause())
          future.fail(ar2.cause())
          return@setHandler
        }

        conn.preparedQuery("update projections set last_uow = $1 where name = $2",
          Tuple.of(uowProjectionData.uowSequence, name)) { ar3 ->

            if (ar3.failed()) {
              future.fail(ar3.cause())
              return@preparedQuery
            }

            // Commit the transaction
            tx.commit { ar4 ->
              if (ar4.succeeded()) {
                log.debug { "Transaction succeeded" }
                future.complete(true)
              } else {
                log.error("Transaction failed", ar4.cause())
                future.fail(ar4.cause())
              }
            }

          }

      }

    }

  }

  private fun listOfutures(size: Int): List<Future<Void>> {
    val list = arrayListOf<Future<Void>>()
    for (i in 0 until size) {
      list.add(Future.future())
    }
    return list.toList()
  }

}

