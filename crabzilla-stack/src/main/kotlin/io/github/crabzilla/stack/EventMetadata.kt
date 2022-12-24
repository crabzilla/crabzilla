package io.github.crabzilla.stack

import io.vertx.core.json.JsonObject

data class EventMetadata(
  val stateType: String,
  val stateId: String,
  val eventId: String,
  val correlationId: String,
  val causationId: String,
  val eventSequence: Long,
  val version: Int,
  val eventType: String
  // TODO app metadata
) {
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("stateType", this.stateType)
      .put("stateId", this.stateId)
      .put("eventSequence", this.eventSequence)
      .put("eventId", this.eventId)
      .put("causationId", this.causationId)
      .put("correlationId", this.correlationId)
      .put("version", this.version)
      .put("eventType", this.eventType)
  }
}

