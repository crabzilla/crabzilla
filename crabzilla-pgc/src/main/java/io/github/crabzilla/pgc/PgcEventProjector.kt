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

  init {
    log.info("starting events projector for $name")
  }

  fun handle(uowProjectionData: ProjectionData, projectorHandler: ProjectorHandler,
             handler: Handler<AsyncResult<Void>>) {

    if (uowProjectionData.events.size > NUMBER_OF_FUTURES) {
      handler.handle(Future.failedFuture("only $NUMBER_OF_FUTURES events can be projected per transaction"))
      return
    }

    pgPool.begin { event1 ->
      if (event1.succeeded()) {

        val tx = event1.result()

        val futures = listOfFutures(minOf(NUMBER_OF_FUTURES, uowProjectionData.events.size))
        for ((pairIndex, event) in uowProjectionData.events.withIndex()) {
          // invoke the projection handler
          projectorHandler.invoke(tx, uowProjectionData.entityId, event, futures[pairIndex])
        }

        CompositeFuture.join(futures).setHandler { event2 ->

          if (event2.failed()) {
            handler.handle(Future.failedFuture(event2.cause()))
            return@setHandler
          }

          tx.preparedQuery("insert into projections (name, last_uow) " +
            "values ($1, $2) on conflict (name) do update set last_uow = $2",
            Tuple.of(name, uowProjectionData.uowSequence)) { event3 ->

            if (event3.failed()) {
              handler.handle(Future.failedFuture(event3.cause()))
              return@preparedQuery
            }

            // Commit the transaction
            tx.commit { event4 ->
              if (event4.succeeded()) {
                log.trace { "Transaction succeeded" }
                handler.handle(Future.succeededFuture())
              } else {
                log.error("Transaction failed", event4.cause())
                handler.handle(Future.failedFuture(event4.cause()))
              }
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

