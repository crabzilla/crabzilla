package io.github.crabzilla.pgc

import io.github.crabzilla.UnitOfWorkEvents
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.logging.LoggerFactory.getLogger

class PgcEventProjector(private val pgPool: PgPool, val name: String) {

  companion object {
    internal val log = getLogger(PgcEventProjector::class.java)
    const val NUMBER_OF_FUTURES = 6 // same as CompositeFuture limit
  }

  init {
    log.info("starting events projector for $name")
  }

  fun handle(uowEvents: UnitOfWorkEvents, projector: PgcEventProjectorHandler, handler: Handler<AsyncResult<Void>>) {

    if (uowEvents.events.size > NUMBER_OF_FUTURES) {
      handler.handle(Future.failedFuture("only $NUMBER_OF_FUTURES events can be projected per transaction"))
      return
    }

    pgPool.begin { event1 ->

      if (event1.succeeded()) {

        val tx = event1.result()

        val futures = ArrayList<Future<Void>>()
        for (pair in uowEvents.events) {
          futures.add(projector.handle(tx, uowEvents.entityId, pair.second))
        }

        val future: Future<Void> = when (futures.size) {
          1 -> futures[0]
          2 -> futureOf2(futures[0], futures[1])
          3 -> futureOf3(futures[0], futures[1], futures[2])
          4 -> futureOf4(futures[0], futures[1], futures[2], futures[3])
          5 -> futureOf5(futures[0], futures[1], futures[2], futures[3], futures[4])
          6 -> futureOf6(futures[0], futures[1], futures[2], futures[3], futures[4], futures[5])
          else -> Future.succeededFuture()
        }

        future.setHandler { event2 ->
          if (event2.succeeded()) {
            tx.preparedQuery("insert into projections (name, last_uow) " +
              "values ($1, $2) on conflict (name) do update set last_uow = $2",
              Tuple.of(name, uowEvents.uowSequence)) { event3 ->
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
          } else {
            log.error("Error while projecting events", event2.cause())
            handler.handle(Future.failedFuture(event2.cause()))
          }
        }
      }
    }
  }

  private fun futureOf6(f1: Future<Void>, f2: Future<Void>, f3: Future<Void>, f4: Future<Void>, f5: Future<Void>,
                        f6: Future<Void>): Future<Void> {
    return f1.compose { f2 }.compose { f3 }.compose { f4 }.compose { f5 }.compose { f6 }
  }

  private fun futureOf5(f1: Future<Void>, f2: Future<Void>, f3: Future<Void>, f4: Future<Void>,
                        f5: Future<Void>): Future<Void> {
    return f1.compose { f2 } .compose { f3 } .compose { f4 } .compose { f5 }
  }

  private fun futureOf4(f1: Future<Void>, f2: Future<Void>, f3: Future<Void>, f4: Future<Void>): Future<Void> {
    return f1.compose { f2 } .compose { f3 }.compose { f4 }
  }

  private fun futureOf3(f1: Future<Void>, f2: Future<Void>, f3: Future<Void>): Future<Void> {
    return f1.compose { f2 } .compose { f3 }
  }

  private fun futureOf2(f1: Future<Void>, f2: Future<Void>): Future<Void> {
    return f1.compose { f2 }
  }

}

