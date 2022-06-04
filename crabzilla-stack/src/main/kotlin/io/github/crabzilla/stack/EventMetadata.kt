package io.github.crabzilla.stack

import io.vertx.core.json.JsonObject
import java.util.*

data class EventMetadata(
  val stateType: String,
  val stateId: UUID,
  val eventId: UUID,
  val correlationId: UUID,
  val causationId: UUID,
  val eventSequence: Long,
  val version: Int,
  val eventType: String
) {
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("stateType", this.stateType)
      .put("stateId", this.stateId.toString())
      .put("eventSequence", this.eventSequence)
      .put("eventId", this.eventId.toString())
      .put("causationId", this.causationId.toString())
      .put("correlationId", this.correlationId.toString())
      .put("version", this.version)
      .put("eventType", this.eventType)
  }
}

