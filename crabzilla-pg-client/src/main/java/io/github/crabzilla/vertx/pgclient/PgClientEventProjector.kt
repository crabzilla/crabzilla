package io.github.crabzilla.vertx.pgclient

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.vertx.ProjectionData
import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.PgPool
import io.vertx.core.Future
import org.slf4j.LoggerFactory.getLogger
import io.vertx.core.CompositeFuture



class PgClientEventProjector(private val pgPool: PgPool) {

  companion object {

    private val log = getLogger(PgClientEventProjector::class.java)
  }

  fun handle(uowList: List<ProjectionData>,
                      projectorFn: (pgConn: PgConnection, targetId: Int, event: DomainEvent, future: Future<Void>) -> Unit,
                      future: Future<Boolean>) {

    pgPool.getConnection({ ar1 ->
      if (ar1.failed()) {
        log.error("handle", ar1.cause())
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

      try {

        // invoke the projector function for all events
        val groups = uowList
          .flatMap { (_, _, targetId, events) -> events.map { Pair(targetId, it) }}
          .chunked(6)


        print("**** groups size " + groups.size)
        print("**** groups ${groups}")


        lateinit var futures: List<Future<Void>>

        groups.forEach(  {
          futures = futures(minOf(6, it.size))
          print("**** futures ${futures.size}")
          for ((i, pair) in it.withIndex()) {
             print("**** index $i")
            projectorFn.invoke(conn, pair.first, pair.second, futures[i])
           }
        })

        CompositeFuture.join(futures).setHandler { ar2 ->
          if (ar2.succeeded()) {
            // Commit the transaction
            tx.commit { ar3 ->
              if (ar3.succeeded()) {
                log.info("Transaction succeeded")
                future.complete(true)
              } else {
                log.error("Transaction failed " + ar3.cause().message)
                future.fail(ar3.cause())
              }
            }
          } else {
            log.error("CompositeFuture ", ar2.cause().message)
          }
        }

      } catch (e: Exception) {

        tx.rollback()
        future.fail(e)
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
