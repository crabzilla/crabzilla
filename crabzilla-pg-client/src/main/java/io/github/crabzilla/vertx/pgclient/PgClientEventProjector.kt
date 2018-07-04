package io.github.crabzilla.vertx.pgclient

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.vertx.ProjectionData
import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.PgPool
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory.getLogger

class PgClientEventProjector(private val pgPool: PgPool) {

  companion object {

    internal val log = getLogger(PgClientEventProjector::class.java)
    const val numberOfFutures = 6

  }

  fun handle(projectionDataList: List<ProjectionData>,
             projectorFn: (pgConn: PgConnection, targetId: Int, event: DomainEvent, future: Future<Void>) -> Unit,
             future: Future<Boolean>) {

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
            log.error("Transaction failed =  > rollbacked")
          }
        })

        val groups = projectionDataList
          .flatMap { (_, _, targetId, events) -> events.map { Pair(targetId, it) }}
          .chunked(numberOfFutures)

        log.debug { "group size ${groups.size}"}

        lateinit var futures: List<Future<Void>>

        groups.forEach(  {
          futures = futures(minOf(numberOfFutures, it.size))
          log.debug { "futures size ${futures.size}"}
          for ((i, pair) in it.withIndex()) {
            // invoke the projection function
            projectorFn.invoke(conn, pair.first, pair.second, futures[i])
           }
        })

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
            log.error( "CompositeFuture", ar2.cause())
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