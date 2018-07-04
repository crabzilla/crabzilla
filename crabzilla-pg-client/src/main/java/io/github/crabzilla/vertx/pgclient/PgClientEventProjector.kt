package io.github.crabzilla.vertx.pgclient

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.vertx.EventsProjector
import io.github.crabzilla.vertx.ProjectionData
import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.PgPool
import io.vertx.core.Future
import org.slf4j.LoggerFactory.getLogger

class PgClientEventProjector(private val pgPool: PgPool) : EventsProjector<PgConnection> {

  companion object {

    private val log = getLogger(PgClientEventProjector::class.java)
  }

  override fun handle(uowList: List<ProjectionData>,
                      projectorFn: (pgConn: PgConnection, targetId: Int, event: DomainEvent) -> Unit,
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
            return@abortHandler
          }
        })

      try {

        // invoke the projector function for all events
        uowList
          .flatMap { (_, _, targetId, events) -> events.map { Pair(targetId, it) }}
          .forEach { (id, event) -> projectorFn.invoke(conn, id, event) }

        println("***** will commit")

        // Commit the transaction
        tx.commit { ar2 ->
          if (ar2.succeeded()) {
            log.info("Transaction succeeded")
            future.complete(true)
          } else {
            log.error("Transaction failed " + ar2.cause().message)
            future.fail(ar2.cause())
          }
        }

      } catch (e: Exception) {
        future.fail(e)
        tx.rollback()
      }

    })

  }

}
