package io.github.crabzilla.context

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.*

/**
 * An event record
 */
data class EventRecord(val metadata: EventMetadata, val payload: JsonObject) {
  companion object {
    fun List<EventRecord>.toJsonArray() = JsonArray(this.map { it.toJsonObject() })

    fun fromJsonObject(asJsonObject: JsonObject): EventRecord {
      val eventMetadata =
        EventMetadata(
          streamId = asJsonObject.getInteger("streamId"),
          stateType = asJsonObject.getString("stateType"),
          stateId = asJsonObject.getString("stateId"),
          eventId = UUID.fromString(asJsonObject.getString("eventId")),
          correlationId = UUID.fromString(asJsonObject.getString("correlationId")),
          causationId = UUID.fromString(asJsonObject.getString("causationId")),
          eventSequence = asJsonObject.getLong("eventSequence"),
          version = asJsonObject.getInteger("version"),
          eventType = asJsonObject.getString("eventType"),
          createdAt = asJsonObject.getInstant("createdAt"),
        )
      return EventRecord(eventMetadata, asJsonObject.getJsonObject("eventPayload"))
    }
  }

  fun toJsonObject(): JsonObject {
    return metadata.toJsonObject()
      .put("eventPayload", payload)
  }

  fun extract() = Pair(payload, metadata)
}
