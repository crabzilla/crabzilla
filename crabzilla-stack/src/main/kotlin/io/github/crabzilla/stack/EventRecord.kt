package io.github.crabzilla.stack

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * An event record
 */
data class EventRecord(val metadata: EventMetadata, val payload: JsonObject) {

  companion object {
    fun List<EventRecord>.toJsonArray() = JsonArray(this.map { it.toJsonObject() })
    fun fromJsonObject(asJsonObject: JsonObject): EventRecord {
      val eventMetadata = EventMetadata(
        asJsonObject.getString("stateType"),
        asJsonObject.getString("stateId"),
        asJsonObject.getString("eventId"),
        asJsonObject.getString("correlationId"),
        asJsonObject.getString("causationId"),
        asJsonObject.getLong("eventSequence"),
        asJsonObject.getInteger("version"),
        asJsonObject.getString("eventType")
      )
      return EventRecord(eventMetadata, asJsonObject.getJsonObject("eventPayload"))
    }
  }

  fun toJsonObject(): JsonObject {
    return metadata.toJsonObject()
      .put("eventPayload", payload)
  }

  fun extract() = Triple(payload, metadata, metadata.stateId)
}
