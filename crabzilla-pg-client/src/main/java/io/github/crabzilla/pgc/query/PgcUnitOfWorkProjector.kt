package io.github.crabzilla.pgc.query

import io.github.crabzilla.core.command.UnitOfWorkEvents
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

class PgcUnitOfWorkProjector(
  private val pgPool: PgPool,
  val entityName: String,
  val streamId: String,
  private val domainEventProjector: PgcDomainEventProjector
) {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcUnitOfWorkProjector::class.java)
    const val SQL_UPDATE_PROJECTIONS =
      """insert into crabz_projections (ar_name, stream_name, last_uow) values ($1, $2, $3)
         on conflict (ar_name, stream_name) do update set last_uow = $3"""
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
        { domainEventProjector.handle(transaction, uowEvents.entityId, uowEvents.events[index]) }
      }
      val futures: List<() -> Future<Void>> = List(uowEvents.events.size, toFuture)
      val future = futures.fold(Future.succeededFuture()) { previousFuture: Future<Void>,
                                                            currentFuture: () -> Future<Void> ->
        previousFuture.compose { currentFuture.invoke() }
      }

      future
        .onFailure { err ->
          log.error("Error while projecting events. Will perform rollback.", err)
//          transaction.rollback { result -> if (result.failed()) log.error("Doing rollback ", result.cause()) }
          promise.fail(err)
        }
        .onSuccess {
          transaction.preparedQuery(SQL_UPDATE_PROJECTIONS)
            .execute(Tuple.of(entityName, streamId, uowEvents.uowId)) { event3 ->
              if (event3.failed()) {
                promise.fail(event3.cause())
                log.error("Error while updating crabz_projections table. Will perform rollback.", event3.cause())
//                transaction.rollback { result -> if (result.failed()) log.error("Doing rollback ", result.cause()) }
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
