package io.github.crabzilla.pgc

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EVENT_SERIALIZER
import io.github.crabzilla.core.EventBusChannels
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.UnitOfWorkEvents
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory.getLogger

internal val log = getLogger("addProjector")

typealias PgcReadContext = Triple<Vertx, Json, PgPool>

fun addProjector(readContext: PgcReadContext, streamId: String, projector: PgcEventProjector) {
  fun toUnitOfWorkEvents(jsonObject: JsonObject, json: Json): UnitOfWorkEvents {
    val uowId = jsonObject.getLong("uowId")
    val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
    val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
    val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsString)
    return UnitOfWorkEvents(uowId, entityId, events)
  }
  log.info("adding projector for $streamId subscribing on ${EventBusChannels.unitOfWorkChannel}")
  val (vertx, json, readDb) = readContext
  vertx.eventBus().consumer<JsonObject>(EventBusChannels.unitOfWorkChannel) { message ->
    val uowEvents = toUnitOfWorkEvents(message.body(), json)
    val uolProjector = PgcUowProjector(readDb, streamId, projector)
    uolProjector.handle(uowEvents).onComplete { result ->
      if (result.failed()) { // TODO circuit breaker
        log.error("Projection [$streamId] failed: " + result.cause().message)
      }
    }
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

/**
 * This implementation is reactive but also has a limitation: it can project only UnitOfWork with 6 events.
 */
class PgcUowProjector(private val pgPool: PgPool, val name: String, val projector: PgcEventProjector) {

  companion object {
    internal val log = getLogger(PgcUowProjector::class.java)
    const val NUMBER_OF_FUTURES = 6 // same as CompositeFuture limit
    const val SQL_UPDATE_PROJECTIONS = """insert into projections (name, last_uow) values ($1, $2) on conflict (name)
      do update set last_uow = $2"""
  }

  fun handle(uowEvents: UnitOfWorkEvents): Future<Void> {
    if (uowEvents.events.size > NUMBER_OF_FUTURES) {
      return failedFuture("only $NUMBER_OF_FUTURES events can be projected per transaction")
    }
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
