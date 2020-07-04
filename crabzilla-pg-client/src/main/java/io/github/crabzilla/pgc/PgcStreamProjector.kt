package io.github.crabzilla.pgc

import io.github.crabzilla.core.command.EventBusChannels
import io.github.crabzilla.core.command.UnitOfWork
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.AsyncMap
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

class PgcStreamProjector(
  private val vertx: Vertx,
  private val writeModelDb: PgPool,
  val entityName: String,
  val streamId: String
) {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcStreamProjector::class.java)
    private const val UOW_ID = "uow_id"
    private const val AR_ID = "ar_id"
    private const val SELECT_AFTER_UOW_ID = "select uow_id, uow_events, ar_id, version from crabz_units_of_work " +
      " where UOW_ID > $1 and ar_name = $2 order by uow_id"
  }

  val isRunning = AtomicBoolean(false)

  fun handle(uowId: Long, maxRowsPerPage: Int, entityName: String, maxRows: Int): Future<Int> {

    if (isRunning.get()) {
      return Future.succeededFuture(0)
    }

    val promise = Promise.promise<Int>()
    isRunning.set(true)
    log.debug("Starting after $uowId for $entityName using page size $maxRowsPerPage ")

    getMap("$entityName.$streamId.producer.map")
      .onFailure { err -> promise.fail(err) }
      .onSuccess { streamIdempotentMap ->
        writeModelDb.begin { event0 ->
          if (event0.failed()) {
            log.error("when starting transaction", event0.cause())
            promise.fail(event0.cause())
            isRunning.set(false)
            return@begin
          }
          val tx = event0.result()
          var rows = 0
          // get committed events after uowId
          tx.prepare("$SELECT_AFTER_UOW_ID limit $maxRows") { event1 ->
            if (event1.failed()) {
              log.error("when getting committed events after snapshot version", event1.cause())
              promise.fail(event1.cause())
              isRunning.set(false)
              return@prepare
            }
            val pq = event1.result()
            // Fetch N rows at a time
            val stream = pq.createStream(maxRowsPerPage, Tuple.of(uowId, entityName))
            stream.exceptionHandler { err ->
              log.error("Stream error", err)
              isRunning.set(false)
              promise.fail(err)
            }
            stream.handler { row ->
              val currentUowId = row.getLong(UOW_ID)
              val currentEntityId = row.getInteger(AR_ID)
              val eventsAsJsonArray: JsonArray = row.get(JsonArray::class.java, 1)
              streamIdempotentMap.get(currentUowId) { attemptsFuture ->
                if (attemptsFuture.failed()) {
                  log.error("Stream error", attemptsFuture.cause())
                  isRunning.set(false)
                  promise.fail(attemptsFuture.cause())
                  stream.close()
                  return@get
                }
                val attempts = attemptsFuture.result()
                if (attempts != null && attempts > 0) {
                  return@get
                }
                val message = JsonObject()
                  .put("uowId", currentUowId)
                  .put(UnitOfWork.JsonMetadata.ENTITY_NAME, entityName)
                  .put(UnitOfWork.JsonMetadata.ENTITY_ID, currentEntityId)
                  .put(UnitOfWork.JsonMetadata.VERSION, row.getInteger("version"))
                  .put(UnitOfWork.JsonMetadata.EVENTS, eventsAsJsonArray)
                vertx.eventBus().publish(EventBusChannels.streamChannel(entityName, streamId), message)
                rows++
                streamIdempotentMap.put(currentUowId, 1) { putFuture ->
                  if (putFuture.failed()) {
                    log.error("Failed to put $currentUowId on idempotent map")
                  }
                }
              }
            }
            stream.endHandler {
              if (log.isDebugEnabled && rows > 0) log.debug("End of stream")
              // Attempt to commit the transaction
              tx.commit { ar ->
                if (ar.failed()) {
                  log.error("tx.commit", ar.cause())
                  promise.fail(ar.cause())
                  isRunning.set(false)
                } else {
                  if (log.isDebugEnabled && rows > 0) log.debug("tx.commit successfully")
                  isRunning.set(false)
                  promise.complete(rows)
                }
              }
            }
          }
        }
      }

    return promise.future()
  }

  private fun getMap(mapName: String): Future<AsyncMap<Long, Int?>> {
    val promise = Promise.promise<AsyncMap<Long, Int?>>()
    vertx.sharedData().getAsyncMap<Long, Int?>(mapName) { event1 ->
      if (event1.failed()) {
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      promise.complete(event1.result())
    }
    return promise.future()
  }
}

//  // TODO use maxRows and test edge cases
//  fun handle(uowId: Long, maxRowsPerPage: Int, entityName: String, maxRows: Int):
//    Future<Pair<Pair<Throwable, Long>?, Int>> {
//
//    val promise = Promise.promise<Pair<Pair<Throwable, Long>?, Int>>()
//    isRunning.set(true)
//    log.debug("Starting after $uowId for $entityName using page size $maxRowsPerPage ")
//    writeModelDb.begin { event0 ->
//      if (event0.failed()) {
//        log.error("when starting transaction", event0.cause())
//        promise.fail(event0.cause())
//        isRunning.set(false)
//        return@begin
//      }
//      val tx = event0.result()
//      var rows = 0
//      var throwable: Throwable? = null
//      var uowIdWithError: Long? = null
//      // get committed events after uowId
//      tx.prepare("$SELECT_AFTER_UOW_ID limit $maxRows") { event1 ->
//        if (event1.failed()) {
//          log.error("when getting committed events after snapshot version", event1.cause())
//          promise.fail(event1.cause())
//          isRunning.set(false)
//          return@prepare
//        }
//        val pq = event1.result()
//        // Fetch N rows at a time
//        val stream = pq.createStream(maxRowsPerPage, Tuple.of(uowId, entityName))
//        stream.exceptionHandler { err ->
//          log.error("Stream error", err)
//          isRunning.set(false)
//          promise.fail(err)
//        }
//        stream.handler { row ->
//          val currentUowId = row.getLong(UOW_ID)
//          val currentEntityId = row.getInteger(AR_ID)
//          val eventsAsJsonArray: JsonArray = row.get(JsonArray::class.java, 1)
//
//          val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsJsonArray.encode())
//          vertx.eventBus().publish(EventBusChannels.streamChannel(entityName, streamId), events)
//          rows++
//        }
//        stream.endHandler {
//          if (log.isDebugEnabled) log.debug("End of stream")
//          // Attempt to commit the transaction
//          tx.commit { ar ->
//            if (ar.failed()) {
//              log.error("tx.commit", ar.cause())
//              promise.fail(ar.cause())
//              isRunning.set(false)
//            } else {
//              log.debug("tx.commit successfully")
//              if (throwable == null) {
//                promise.complete(Pair(null, rows))
//                isRunning.set(false)
//              } else {
//                isRunning.set(false)
//                promise.complete(Pair(Pair(throwable!!, uowIdWithError!!), rows))
//              }
//            }
//          }
//        }
//      }
//    }
//    return promise.future()
//  }
