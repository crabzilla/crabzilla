package io.github.crabzilla.hazelcast.command

import com.hazelcast.core.ExecutionCallback
import com.hazelcast.ringbuffer.OverflowPolicy.OVERWRITE
import com.hazelcast.ringbuffer.Ringbuffer
import io.github.crabzilla.core.command.UnitOfWorkPublisher
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class HzRingBufferPublisher(val vertx: Vertx, private val ringBuffer: Ringbuffer<String>) : UnitOfWorkPublisher {

  companion object {
    private val log = LoggerFactory.getLogger(HzRingBufferPublisher::class.java)
  }

  override fun publish(events: JsonObject): Future<Long> {
    val promise0 = Promise.promise<Long>()
    if (log.isDebugEnabled) log.debug("will publish to ${ringBuffer.name} these events $events")
    ringBuffer
      .addAsync(events.encode(), OVERWRITE)
      .andThen(HzCallback(promise0))
    return promise0.future()
  }

  private class HzCallback(private val promise: Promise<Long>) : ExecutionCallback<Long> {
    override fun onFailure(t: Throwable) {
      log.error("Reading ring buffer", t)
      promise.fail(t)
    }
    override fun onResponse(sequence: Long) {
      promise.complete(sequence)
    }
  }
}
