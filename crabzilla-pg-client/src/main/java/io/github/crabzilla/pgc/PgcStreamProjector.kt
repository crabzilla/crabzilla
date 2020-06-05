package io.github.crabzilla.pgc

import io.github.crabzilla.core.command.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.core.command.UnitOfWorkEvents
import io.github.crabzilla.pgc.query.PgcUnitOfWorkProjector
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class PgcStreamProjector(
  private val writeModelDb: PgPool,
  private val json: Json,
  private val projector: PgcUnitOfWorkProjector
) {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcStreamProjector::class.java)
    private const val UOW_ID = "uow_id"
    private const val AR_ID = "ar_id"
    private const val SELECT_AFTER_UOW_ID = "select uow_id, uow_events, ar_id from units_of_work " +
      " where UOW_ID > $1 and ar_name = $2 order by uow_id"
  }

  fun entityName(): String {
    return projector.entityName
  }

  fun streamId(): String {
    return projector.streamId
  }

  // TODO use maxRows and test
  fun handle(uowId: Long, maxRowsPerPage: Int, entityName: String, maxRows: Int): Future<Long> {
    val promise = Promise.promise<Long>()
    log.debug("Starting after $uowId for $entityName using page size $maxRowsPerPage ")
    writeModelDb.begin { event0 ->
      if (event0.failed()) {
        log.error("when starting transaction", event0.cause())
        promise.fail(event0.cause())
        return@begin
      }
      val tx = event0.result()
      var rows = 0L
      // get committed events after uowId
      tx.prepare(SELECT_AFTER_UOW_ID) { event1 ->
        if (event1.failed()) {
          log.error("when getting committed events after snapshot version", event1.cause())
          promise.fail(event1.cause())
          return@prepare
        }
        val pq = event1.result()
        // Fetch N rows at a time
        val stream = pq.createStream(maxRowsPerPage, Tuple.of(uowId, entityName))
        stream.exceptionHandler { err ->
          promise.fail(err)
          log.error("Stream error", err) }
        stream.handler { row ->
          rows++
          log.info(row.deepToString())
          val currentUowId = row.getLong(UOW_ID)
          val currentEntityId = row.getInteger(AR_ID)
          val eventsAsJsonArray: JsonArray = row.get(JsonArray::class.java, 1)
          val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsJsonArray.encode())
          val sideEffect: Future<Void> = projector.handle(UnitOfWorkEvents(currentUowId, currentEntityId, events))
            .onFailure { err -> log.error("When projecting uowId $currentUowId", err) }
          if (sideEffect.failed()) {
            throw sideEffect.cause() // fail immediately and hopefully abort the transaction // TODO test this
          }
        }
        stream.endHandler {
          if (log.isDebugEnabled) log.debug("End of stream")
          // Attempt to commit the transaction
          tx.commit { ar ->
            if (ar.failed()) {
              log.error("tx.commit", ar.cause())
              promise.fail(ar.cause())
            } else {
              log.debug("tx.commit successfully")
              promise.complete(rows)
            }
          }
        }
      }
    }
    return promise.future()
  }
}
