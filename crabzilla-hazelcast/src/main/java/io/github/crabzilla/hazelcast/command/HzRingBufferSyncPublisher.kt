package io.github.crabzilla.hazelcast.command

import com.hazelcast.ringbuffer.Ringbuffer
import io.github.crabzilla.core.command.UnitOfWorkPublisher
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class HzRingBufferSyncPublisher(val vertx: Vertx, private val ringBuffer: Ringbuffer<String>) : UnitOfWorkPublisher {
  companion object {
    private val log = LoggerFactory.getLogger(HzRingBufferPublisher::class.java)
  }
  override fun publish(events: JsonObject): Future<Long> {
    val promise0 = Promise.promise<Long>()
    if (log.isDebugEnabled) log.debug("will publish to ${ringBuffer.name} these events $events")
    vertx.executeBlocking<Long>({ promise ->
      val sequence = ringBuffer.add(events.encode())
      promise.complete(sequence)
    }, { res ->
      if (res.failed()) {
        log.error("Failed ", res.cause())
        promise0.fail(res.cause())
        return@executeBlocking
      }
      val seq = res.result()
      log.info("Sequence $seq")
      if (seq == null) {
        promise0.complete(0)
      } else {
        promise0.complete(seq)
      }
    })
    return promise0.future()
  }
}
