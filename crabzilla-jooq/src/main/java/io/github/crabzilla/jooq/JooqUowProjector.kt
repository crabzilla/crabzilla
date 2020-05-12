package io.github.crabzilla.jooq

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnitOfWorkEvents
import io.github.crabzilla.jooq.datamodel.Tables
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Promise
import org.jooq.DSLContext
import org.jooq.Query
import org.slf4j.LoggerFactory.getLogger

class JooqUowProjector(
  private val executor: ReactiveClassicGenericQueryExecutor,
  private val streamId: String,
  private val projector: (DomainEvent, Int) -> ((DSLContext) -> Query)?
) {
  companion object {
    internal val log = getLogger(JooqUowProjector::class.java)
    const val NUMBER_OF_FUTURES = 10 // not limited by CompositeFuture limit :)
  }
  fun handle(uowEvents: UnitOfWorkEvents): Future<Int> {
    if (uowEvents.events.size > NUMBER_OF_FUTURES) {
      return failedFuture("Only $NUMBER_OF_FUTURES events can be projected per transaction")
    }
    val toJooqSideEffectFn: (Int) -> ((ReactiveClassicGenericQueryExecutor, Int) -> Future<Int>)? = { index ->
      convert(uowEvents.events[index], uowEvents.entityId, projector)
    }
    val sideEffects: List<(ReactiveClassicGenericQueryExecutor, Int) -> Future<Int>> =
      List(uowEvents.events.size, toJooqSideEffectFn).filterNotNull() // removing events without any projection side effect
    if (sideEffects.isEmpty()) {
      return succeededFuture(0)
    }
    val firstOp: (DSLContext) -> Query = { dslContext ->
      dslContext
        .insertInto(Tables.PROJECTIONS)
        .columns(Tables.PROJECTIONS.NAME, Tables.PROJECTIONS.LAST_UOW)
        .values(streamId, uowEvents.uowId.toInt()) // TODO remove toInt
        .onDuplicateKeyUpdate()
        .set(Tables.PROJECTIONS.LAST_UOW, uowEvents.uowId.toInt())
    }
    val promise = Promise.promise<Int>()
    executor.transaction { tx: ReactiveClassicGenericQueryExecutor ->
      tx.execute(firstOp)
        .compose { i: Int -> sideEffects[0].invoke(tx, i) }
        .compose { i: Int -> if (sideEffects.size > 1) sideEffects[1].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (sideEffects.size > 2) sideEffects[2].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (sideEffects.size > 3) sideEffects[3].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (sideEffects.size > 4) sideEffects[4].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (sideEffects.size > 5) sideEffects[5].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (sideEffects.size > 6) sideEffects[6].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (sideEffects.size > 7) sideEffects[7].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (sideEffects.size > 8) sideEffects[8].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (sideEffects.size > 9) sideEffects[9].invoke(tx, i) else succeededFuture(0) }
        .onSuccess { count ->
          if (log.isDebugEnabled) log.debug("Projection success, rows = $count")
          promise.complete(count) }
        .onFailure { err ->
          log.error("Projection error", err)
          promise.fail(err)
        }
    }
    return promise.future()
  }

  private fun convert(event: DomainEvent, targetId: Int, projector: (DomainEvent, Int) -> ((DSLContext) -> Query)?):
    ((ReactiveClassicGenericQueryExecutor, Int) -> Future<Int>)? {
    val projectionSideEffect = projector.invoke(event, targetId) ?: return null
    return { tx: ReactiveClassicGenericQueryExecutor, _: Int -> tx.execute(projectionSideEffect) }
  }
}
