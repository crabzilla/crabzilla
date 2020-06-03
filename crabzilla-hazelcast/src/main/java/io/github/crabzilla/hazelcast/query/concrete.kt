package io.github.crabzilla.hazelcast.query

import com.hazelcast.core.ICompletableFuture
import com.hazelcast.ringbuffer.ReadResultSet
import com.hazelcast.ringbuffer.Ringbuffer
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EVENT_SERIALIZER
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.UnitOfWorkEvents
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.impl.headers.VertxHttpHeaders
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.AsyncMap
import io.vertx.kotlin.core.json.jsonArrayOf
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory.getLogger

data class DomainEventMessage(val uowId: Long, val entityId: Int, val event: DomainEvent)

class HzProjectionRepo(private val map: AsyncMap<String, Long>) {

  companion object {
    private val log = getLogger(HzProjectionRepo::class.java)
  }

  fun selectLastSequence(streamId: String): Future<Long> {
    val promise = Promise.promise<Long>()
    map.get(streamId) { event2 ->
      if (event2.failed()) {
        log.error("Failed to get $streamId on map $map")
        promise.fail(event2.cause())
        return@get
      }
      val result = event2.result()
      if (result == null) {
        if (log.isDebugEnabled) {
          log.debug("Last sequence for $streamId on map $map: ${result ?: 0L}")
        }
        promise.complete(result ?: 0L)
      }
    }
    return promise.future()
  }
}

fun addHzProjector(
  vertx: Vertx,
  entityName: String,
  streamId: String,
  hzProjectionRepo: HzProjectionRepo,
  ringBuffer: Ringbuffer<JsonObject>,
  json: Json,
  projector: HzProjectionHandler
) {

  val log = getLogger("addHzProjector$entityName-$streamId")

  fun pullSourceThenMoveToSink(sequence: Long, maxElements: Int) {
    val future: ICompletableFuture<ReadResultSet<JsonObject>> = ringBuffer.readManyAsync(sequence, 1, maxElements, null)
    val rs: ReadResultSet<JsonObject> = future.get()
    val jsonArray = jsonArrayOf(rs.toList())
    val headers = VertxHttpHeaders().set("sequence", sequence)
    val deliveryOptions = DeliveryOptions().setHeaders(headers).setLocalOnly(true)
    vertx.eventBus().publish(streamId, jsonArray, deliveryOptions)
  }

  fun toUnitOfWorkEvents(jsonObject: JsonObject): UnitOfWorkEvents {
    val uowId = jsonObject.getLong("uowId")
    val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
    val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
    val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsAsString)
    return UnitOfWorkEvents(uowId, entityId, events)
  }

  log.info("adding projector for entityName $entityName streamId $streamId")

  // side effect
  vertx.eventBus().consumer<JsonArray>(streamId) { message ->
    message.body()
      .map { toUnitOfWorkEvents(it as JsonObject) }
      .flatMap { it.events.map { event -> DomainEventMessage(it.uowId, it.entityId, event) } }
      .onEach { projector.handle(message.headers().get("sequence").toLong(), it) }
  }

  // on startup
  hzProjectionRepo.selectLastSequence(streamId)
    .onSuccess { sequence -> pullSourceThenMoveToSink(sequence, Int.MAX_VALUE)
      // reactive
      vertx.eventBus().consumer<Void>(entityName) {
        val headSequence = ringBuffer.headSequence()
        pullSourceThenMoveToSink(headSequence, 100)
      }
    }
    .onFailure { err -> err.printStackTrace() } // TODO it should fail on startup
}
