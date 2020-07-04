package io.github.crabzilla.hazelcast.query

import com.hazelcast.core.ExecutionCallback
import com.hazelcast.core.ICompletableFuture
import com.hazelcast.ringbuffer.ReadResultSet
import com.hazelcast.ringbuffer.Ringbuffer
import io.github.crabzilla.core.command.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.core.command.UnitOfWork
import io.github.crabzilla.core.command.UnitOfWorkEvents
import io.github.crabzilla.pgc.query.PgcUnitOfWorkProjector
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.util.concurrent.TimeUnit
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class HzStreamConsumer( // TODO use a builder dsl
  val vertx: Vertx,
  val entityName: String,
  val streamId: String,
  val uolProjector: PgcUnitOfWorkProjector,
  val ringBuffer: Ringbuffer<String>,
  val json: Json,
  val hzProjectionRepo: HzProjectionRepo
) {

  companion object {
    private val log = LoggerFactory.getLogger(HzStreamConsumer::class.java)
  }

  fun start() {
    vertx.setTimer(1000, handler)
  }

  private val handler: Handler<Long> = Handler {
    pullEvents()
  }

  private fun pullEvents() {

    fun pullRingBuffer(maxElements: Int): Future<List<String>> {
      val promise = Promise.promise<List<String>>()
      val future: ICompletableFuture<ReadResultSet<String>> =
        ringBuffer.readManyAsync(ringBuffer.headSequence(), 1, maxElements, null)
      future.andThen(HzCallback(promise))
      return promise.future()
    }

    fun toUnitOfWorkEvents(jsonObject: JsonObject): UnitOfWorkEvents {
      val uowId = jsonObject.getLong("uowId")
      val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
      val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
      val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsString)
      return UnitOfWorkEvents(uowId, entityId, events)
    }

    pullRingBuffer(1000)
      .onFailure { err ->
        vertx.setTimer(TimeUnit.SECONDS.toMillis(1), handler)
        log.error("pullRingBuffer", err) }
      .onSuccess { eventsList: List<String> ->
        val futures: List<() -> Future<Void>> = eventsList
          .map { JsonObject(it) }
          .map { toUnitOfWorkEvents(it) }
          .map { uowEvents -> { uolProjector.handle(uowEvents) } }
          .toList()
        val reducedFuture: Future<Void> = futures
          .fold(Future.succeededFuture()) { previousFuture: Future<Void>, currentFuture: () -> Future<Void> ->
          previousFuture.compose {
            if (previousFuture.failed()) {
              Future.failedFuture(previousFuture.cause())
            } else {
              currentFuture.invoke()
            }
          }
        }
        reducedFuture.onFailure { err ->
            vertx.setTimer(TimeUnit.SECONDS.toMillis(1), handler)
            log.error("uolProjector.handle(uowEvents)", err)
          }
          .onSuccess {
            vertx.setTimer(TimeUnit.SECONDS.toMillis(1), handler)
          }
      }
  }

  private class HzCallback(private val promise: Promise<List<String>>) : ExecutionCallback<ReadResultSet<String>> {
    override fun onFailure(t: Throwable) {
      promise.fail(t)
    }
    override fun onResponse(response: ReadResultSet<String>) {
      promise.complete(response.toList())
    }
  }
}
