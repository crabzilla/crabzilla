package io.github.crabzilla.stack

import io.github.crabzilla.core.metadata.EventMetadata
import io.vertx.core.json.JsonObject
import java.util.UUID

/**
 * An event record
 */
data class EventRecord(
  val eventMetadata: EventMetadata,
  val eventAsjJson: JsonObject,
) {
  companion object {
    private fun JsonObject.getUUID(name: String): UUID {
      return UUID.fromString(this.getString(name))
    }
    fun fromJsonObject(asJsonObject: JsonObject): EventRecord {
      val eventMetadata = EventMetadata(
        asJsonObject.getString("aggregateName"),
        asJsonObject.getUUID("aggregateId"),
        asJsonObject.getUUID("eventId"),
        asJsonObject.getUUID("correlationId"),
        asJsonObject.getUUID("causationId"),
        asJsonObject.getLong("eventSequence")
      )
      return EventRecord(eventMetadata, asJsonObject.getJsonObject("eventAsjJson"))
    }
  }
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("eventAsjJson", eventAsjJson)
      .put("aggregateName", eventMetadata.stateType)
      .put("aggregateId", eventMetadata.stateId.toString())
      .put("eventSequence", eventMetadata.eventSequence)
      .put("eventId", eventMetadata.eventId.toString())
      .put("causationId", eventMetadata.causationId.toString())
      .put("correlationId", eventMetadata.correlationId.toString())
  }
}
