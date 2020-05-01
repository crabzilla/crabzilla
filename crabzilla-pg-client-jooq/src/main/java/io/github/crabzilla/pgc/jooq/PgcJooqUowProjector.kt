package io.github.crabzilla.pgc.jooq

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.internal.UnitOfWorkEvents
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import org.jooq.Configuration
import org.slf4j.LoggerFactory.getLogger

/*  TODO vertx-jooq implementation

 ReactiveClassicGenericQueryExecutor nonTransactionalQueryExecutor...;
  Future<QueryResult> resultOfTransaction = nonTransactionalQueryExecutor.transaction(transactionalQueryExecutor ->{

    //make all calls on the provided QueryExecutor that runs all code in a transaction
    return transactionalQueryExecutor.execute(dslContext -> dslContext.insertInto(Tables.XYZ)...)
            .compose(i -> transactionalQueryExecutor.query(dslContext ->
                              dslContext.selectFrom(Tables.XYZ).where(Tables.XYZ.SOME_VALUE.eq("FOO")));
    }
  );

 */
class PgcJooqUowProjector(private val jooq: Configuration, private val pgPool: PgPool, val name: String) {

  companion object {
    internal val log = getLogger(PgcJooqUowProjector::class.java)
    const val NUMBER_OF_FUTURES = 6 // same as CompositeFuture limit
    const val SQL_UPDATE_PROJECTIONS = """insert into projections (name, last_uow) values ($1, $2) on conflict (name)
      do update set last_uow = $2"""
  }

  fun handle(uowEvents: UnitOfWorkEvents, projector: PgcEventProjector): Future<Void> {
    if (uowEvents.events.size > NUMBER_OF_FUTURES) {
      return failedFuture("only $NUMBER_OF_FUTURES events can be projected per transaction")
    }
    val vertxJooq = ReactiveClassicGenericQueryExecutor(jooq, pgPool) // TODO

    val promise = Promise.promise<Void>()
    pgPool.begin { event1 ->
      if (event1.failed()) {
        log.error("Error when starting transaction", event1.cause())
        promise.fail(event1.cause())
        return@begin
      }
      val tx = event1.result()
      val toFuture: (Int) -> Future<Void> = { index ->
        projector.handle(tx, uowEvents.entityId, uowEvents.events[index])
      }
      val futures: List<Future<Void>> = List(uowEvents.events.size, toFuture)
      val future: Future<Void> = when (futures.size) {
        1 -> futures[0]
        2 -> futureOf2(futures[0], futures[1])
        3 -> futureOf3(futures[0], futures[1], futures[2])
        4 -> futureOf4(futures[0], futures[1], futures[2], futures[3])
        5 -> futureOf5(futures[0], futures[1], futures[2], futures[3], futures[4])
        6 -> futureOf6(futures[0], futures[1], futures[2], futures[3], futures[4], futures[5])
        else -> failedFuture("More than 6 events")
      }
      future.onComplete { event2 ->
        if (event2.failed()) {
          log.error("Error while projecting events", event2.cause())
          promise.fail(event2.cause())
          return@onComplete
        }
        tx.preparedQuery(SQL_UPDATE_PROJECTIONS)
          .execute(Tuple.of(name, uowEvents.uowId)) { event3 ->
          if (event3.failed()) {
            promise.fail(event3.cause())
            return@execute
          }
          // Commit the transaction
          tx.commit { event4 ->
            if (event4.failed()) {
              if (log.isDebugEnabled) log.error("Transaction failed", event4.cause())
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

  private fun futureOf6(
    f1: Future<Void>,
    f2: Future<Void>,
    f3: Future<Void>,
    f4: Future<Void>,
    f5: Future<Void>,
    f6: Future<Void>
  ): Future<Void> {
    return f1.compose { f2 }.compose { f3 }.compose { f4 }.compose { f5 }.compose { f6 }
  }

  private fun futureOf5(
    f1: Future<Void>,
    f2: Future<Void>,
    f3: Future<Void>,
    f4: Future<Void>,
    f5: Future<Void>
  ): Future<Void> {
    return f1.compose { f2 }.compose { f3 }.compose { f4 }.compose { f5 }
  }

  private fun futureOf4(f1: Future<Void>, f2: Future<Void>, f3: Future<Void>, f4: Future<Void>): Future<Void> {
    return f1.compose { f2 }.compose { f3 }.compose { f4 }
  }

  private fun futureOf3(f1: Future<Void>, f2: Future<Void>, f3: Future<Void>): Future<Void> {
    return f1.compose { f2 }.compose { f3 }
  }

  private fun futureOf2(f1: Future<Void>, f2: Future<Void>): Future<Void> {
    return f1.compose { f2 }
  }
}

interface PgcEventProjector {

  fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void>

  fun executePreparedQuery(tx: Transaction, query: String, tuple: Tuple): Future<Void> {
    val promise = Promise.promise<Void>()

    tx.preparedQuery(query)
      .execute(tuple) { event ->
        if (event.failed()) {
          promise.fail(event.cause())
        } else {
          promise.complete()
        }
      }
    return promise.future()
  }
}
