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
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit.SECONDS

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

    fun pullRingBuffer(sequence: Long, maxElements: Int): Future<List<String>> {
      val promise = Promise.promise<List<String>>()
      val future: ICompletableFuture<ReadResultSet<String>> =
        ringBuffer.readManyAsync(sequence, 1, maxElements, null)
      future.andThen(HzCallback(promise))
      return promise.future()
    }

    fun pullRingBufferBlocking(sequence: Long, maxElements: Int): Future<List<String>> {
      val promise0 = Promise.promise<List<String>>()
      vertx.executeBlocking<ReadResultSet<String>>({ promise ->
        val future: ICompletableFuture<ReadResultSet<String>> =
          ringBuffer.readManyAsync(sequence, 1, maxElements, null)
        val rs = try {
          future.get(1, SECONDS)
        } catch (e: Exception) {
          null
        }
        if (rs == null) {
          promise.complete()
        } else {
          promise.complete(rs)
        }
      }, true, { res ->
        if (res.failed()) {
          promise0.fail(res.cause())
          return@executeBlocking
        }
        val resultSet = res.result()
        if (resultSet == null) {
          promise0.complete(listOf())
        } else {
          promise0.complete(res.result().toList())
        }
        log.info("The result is: ${res.result()}")
      })
      return promise0.future()
    }

    fun toUnitOfWorkEvents(jsonObject: JsonObject): UnitOfWorkEvents {
      val uowId = jsonObject.getLong("uowId")
      val entityId = jsonObject.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
      val eventsAsString = jsonObject.getJsonArray(UnitOfWork.JsonMetadata.EVENTS).encode()
      val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsString)
      return UnitOfWorkEvents(uowId, entityId, events)
    }

    vertx.setPeriodic(1000) {
      hzProjectionRepo.getLastSequence(entityName, streamId)
        .onFailure { err -> log.error("hzProjectionRepo.getLastSequence", err) }
        .onSuccess { lastSequence ->
          var rows = lastSequence
          pullRingBuffer(lastSequence, 100)
            .onFailure { err -> log.error("pullRingBuffer", err) }
            .onSuccess { list ->
              list
                .map { JsonObject(it) }
                .map { toUnitOfWorkEvents(it) }
                .forEach { uowEvents ->
                  uolProjector.handle(uowEvents)
                    .onFailure { err -> log.error("uolProjector.handle", err) }
                    .onSuccess {
                      /*log.info("Success")*/
                      rows += 1
                      hzProjectionRepo.setLastSequence(entityName, streamId, rows)
                        .onFailure { err -> log.error("hzProjectionRepo.setLastSequence", err) }
                        .onSuccess {
                          /*log.info("Success")*/
                        }
                    }
                }
            }
        }
    }
  }

  private class HzCallback(private val promise2: Promise<List<String>>): ExecutionCallback<ReadResultSet<String>> {
    override fun onFailure(t: Throwable) {
      promise2.fail(t)
    }
    override fun onResponse(response: ReadResultSet<String>) {
      promise2.complete(response.toList())
    }
  }

}

