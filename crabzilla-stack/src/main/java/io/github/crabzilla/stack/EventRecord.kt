package io.github.crabzilla.stack

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
    fun fromJsonObject(asJsonObject: JsonObject): EventRecord {
      val eventMetadata = EventMetadata(
        asJsonObject.getString("aggregateName"),
        DomainStateId(UUID.fromString(asJsonObject.getString("aggregateId"))),
        EventId(UUID.fromString(asJsonObject.getString("eventId"))),
        CorrelationId(UUID.fromString(asJsonObject.getString("correlationId"))),
        CausationId(UUID.fromString(asJsonObject.getString("causationId"))),
        asJsonObject.getLong("eventSequence")
      )
      return EventRecord(eventMetadata, asJsonObject.getJsonObject("eventAsjJson"))
    }
  }
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("eventAsjJson", eventAsjJson)
      .put("aggregateName", eventMetadata.aggregateName)
      .put("aggregateId", eventMetadata.domainStateId.id.toString())
      .put("eventSequence", eventMetadata.eventSequence)
      .put("eventId", eventMetadata.eventId.id.toString())
      .put("causationId", eventMetadata.causationId.id.toString())
      .put("correlationId", eventMetadata.correlationId.id.toString())
  }
}
