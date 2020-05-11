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
    val toFut: (Int) -> ((ReactiveClassicGenericQueryExecutor, Int) -> Future<Int>)? = { index ->
      futureIntFn(uowEvents.events[index], uowEvents.entityId, projector)
    }
    val futures: List<(ReactiveClassicGenericQueryExecutor, Int) -> Future<Int>> = List(uowEvents.events.size, toFut)
      .filterNotNull() // removing events without any projection side effect
    if (futures.isEmpty()) {
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
        .compose { i: Int -> futures[0].invoke(tx, i) }
        .compose { i: Int -> if (futures.size > 1) futures[1].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (futures.size > 2) futures[2].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (futures.size > 3) futures[3].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (futures.size > 4) futures[4].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (futures.size > 5) futures[5].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (futures.size > 6) futures[6].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (futures.size > 7) futures[7].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (futures.size > 8) futures[8].invoke(tx, i) else succeededFuture(0) }
        .compose { i: Int -> if (futures.size > 9) futures[9].invoke(tx, i) else succeededFuture(0) }
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

  private fun futureIntFn(event: DomainEvent, targetId: Int, projector: (DomainEvent, Int) -> ((DSLContext) -> Query)?):
    ((ReactiveClassicGenericQueryExecutor, Int) -> Future<Int>)? {
    val projectionSideEffect = projector.invoke(event, targetId) ?: return null
    return { tx: ReactiveClassicGenericQueryExecutor, _: Int -> tx.execute(projectionSideEffect) }
  }
}
