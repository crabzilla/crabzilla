package io.github.crabzilla.hazelcast.command

import com.hazelcast.ringbuffer.Ringbuffer
import io.github.crabzilla.core.UnitOfWorkPublisher
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class HzRingBufferPublisher(
  private val vertx: Vertx,
  private val entityName: String,
  private val ringBuffer: Ringbuffer<JsonObject>
) : UnitOfWorkPublisher {
  companion object {
    private val log = LoggerFactory.getLogger(HzRingBufferPublisher::class.java)
  }
  override fun publish(events: JsonObject) {
    if (log.isDebugEnabled) log.debug("will publish $events")
    val sequence = ringBuffer.add(events)
    // TODO save uowId x sequence on a shared map
    vertx.eventBus().publish(entityName, null)
  }
}
