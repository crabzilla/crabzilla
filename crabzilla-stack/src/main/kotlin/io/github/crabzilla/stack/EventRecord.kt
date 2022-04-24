package io.github.crabzilla.stack

import io.vertx.core.json.JsonObject
import java.util.UUID

/**
 * An event record
 */
data class EventRecord(val metadata: EventMetadata, val payload: JsonObject) {

  companion object {
    fun fromJsonObject(asJsonObject: JsonObject): EventRecord {
      val eventMetadata = EventMetadata(
        asJsonObject.getString("stateType"),
        UUID.fromString(asJsonObject.getString("stateId")),
        UUID.fromString(asJsonObject.getString("eventId")),
        UUID.fromString(asJsonObject.getString("correlationId")),
        UUID.fromString(asJsonObject.getString("causationId")),
        asJsonObject.getLong("eventSequence"),
        asJsonObject.getInteger("version")
      )
      return EventRecord(eventMetadata, asJsonObject.getJsonObject("eventPayload"))
    }
  }

  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("eventPayload", payload)
      .put("stateType", metadata.stateType)
      .put("stateId", metadata.stateId.toString())
      .put("eventSequence", metadata.eventSequence)
      .put("eventId", metadata.eventId.toString())
      .put("causationId", metadata.causationId.toString())
      .put("correlationId", metadata.correlationId.toString())
      .put("version", metadata.version)
  }

  fun extract() = Triple(payload, metadata, metadata.stateId)
}
