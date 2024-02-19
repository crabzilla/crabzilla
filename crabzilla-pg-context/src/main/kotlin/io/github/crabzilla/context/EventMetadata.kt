package io.github.crabzilla.context

import io.vertx.core.json.JsonObject
import java.time.Instant
import java.util.*

data class EventMetadata(
  val streamId: Int,
  val stateType: String,
  val stateId: String,
  val eventId: UUID,
  val correlationId: UUID,
  val causationId: UUID,
  val eventSequence: Long,
  val version: Int,
  val eventType: String,
  val createdAt: Instant,
) {
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("streamId", this.streamId)
      .put("stateType", this.stateType)
      .put("stateId", this.stateId)
      .put("eventSequence", this.eventSequence)
      .put("eventId", this.eventId.toString())
      .put("causationId", this.causationId.toString())
      .put("correlationId", this.correlationId.toString())
      .put("version", this.version)
      .put("eventType", this.eventType)
      .put("createdAt", this.createdAt)
  }
}
