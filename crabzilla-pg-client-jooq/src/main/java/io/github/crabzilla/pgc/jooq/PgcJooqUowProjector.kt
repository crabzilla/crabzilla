package io.github.crabzilla.pgc.jooq

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnitOfWorkEvents
import io.github.crabzilla.pgc.jooq.datamodel.Tables
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.Query
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.slf4j.LoggerFactory.getLogger

class PgcJooqUowProjector(
  private val pgPool: PgPool,
  private val streamId: String,
  private val projector: (DomainEvent, Int) -> (DSLContext) -> Query,
  private val jooq: Configuration = DefaultConfiguration()
) {

  companion object {
    internal val log = getLogger(PgcJooqUowProjector::class.java)
    const val NUMBER_OF_FUTURES = 10 // not limited by CompositeFuture limit :)
  }

  init {
    jooq.set(SQLDialect.POSTGRES)
  }

  fun handle(uowEvents: UnitOfWorkEvents): Future<Int> {
    if (uowEvents.events.size > NUMBER_OF_FUTURES) {
      return failedFuture("only $NUMBER_OF_FUTURES events can be projected per transaction")
    }
    val promise = Promise.promise<Int>()
    val toFut: (Int) -> (ReactiveClassicGenericQueryExecutor, Int) -> Future<Int> = { index ->
      convert(uowEvents.events[index], uowEvents.entityId, projector)
    }
    val futures: List<(ReactiveClassicGenericQueryExecutor, Int) -> Future<Int>> = List(uowEvents.events.size, toFut)
    val firstOp: (DSLContext) -> Query = { dslContext ->
      dslContext
        .insertInto(Tables.PROJECTIONS)
        .columns(Tables.PROJECTIONS.NAME, Tables.PROJECTIONS.LAST_UOW)
        .values(streamId, uowEvents.uowId.toInt()) // TODO remove toInt
        .onDuplicateKeyUpdate()
        .set(Tables.PROJECTIONS.LAST_UOW, uowEvents.uowId.toInt())
    }
    ReactiveClassicGenericQueryExecutor(jooq, pgPool).transaction { tx: ReactiveClassicGenericQueryExecutor ->
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

  private fun convert(event: DomainEvent, targetId: Int, pFn: (DomainEvent, Int) -> (DSLContext) -> Query):
    (ReactiveClassicGenericQueryExecutor, Int) -> Future<Int> {
    return { tx: ReactiveClassicGenericQueryExecutor, _: Int -> tx.execute(pFn.invoke(event, targetId)) }
  }
}
