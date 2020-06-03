package io.github.crabzilla.pgc.query

import io.github.crabzilla.core.UnitOfWorkEvents
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

class PgcUowProjector(
  private val pgPool: PgPool,
  private val entityName: String,
  private val streamId: String,
  private val projector: PgcEventProjector
) {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcUowProjector::class.java)
    const val SQL_UPDATE_PROJECTIONS =
      """insert into projections (entityName, streamId, last_uow) values ($1, $2, $3) on conflict (entityName, streamId)
      do update set last_uow = $3"""
  }

  fun handle(uowEvents: UnitOfWorkEvents): Future<Void> {
    val promise = Promise.promise<Void>()

    pgPool.begin { event1 ->
      if (event1.failed()) {
        log.error("Error when starting transaction", event1.cause())
        promise.fail(event1.cause())
        return@begin
      }
      val transaction = event1.result()

      val toFuture: (Int) -> () -> Future<Void> = { index ->
        { projector.handle(transaction, uowEvents.entityId, uowEvents.events[index]) }
      }
      val futures: List<() -> Future<Void>> = List(uowEvents.events.size, toFuture)
      val future = futures.fold(Future.succeededFuture()) { previousFuture: Future<Void>,
                                                            currentFuture: () -> Future<Void> ->
        previousFuture.compose { currentFuture.invoke() }
      }

      future
        .onFailure { err ->
          transaction.rollback { result -> if (result.failed()) log.error("Doing rollback ", result.cause()) }
          log.error("Error while projecting events", err)
          promise.fail(err)
        }
        .onSuccess {
          transaction.preparedQuery(SQL_UPDATE_PROJECTIONS)
            .execute(Tuple.of(entityName, streamId, uowEvents.uowId)) { event3 ->
              if (event3.failed()) {
                promise.fail(event3.cause())
                transaction.rollback { result -> if (result.failed()) log.error("Doing rollback ", result.cause()) }
                log.error("Error while updating projections table", event3.cause())
                return@execute
              }
              // Commit the transaction
              transaction.commit { event4 ->
                if (event4.failed()) {
                  log.error("Transaction commit failed", event4.cause())
                  promise.fail(event4.cause())
                  return@commit
                }
                if (log.isDebugEnabled) log.debug("Transaction succeeded")
                promise.complete()
              }
            }
        }
    }
    return promise.future()
  }
}
