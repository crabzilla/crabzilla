package io.github.crabzilla.projection

import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.EventId
import io.github.crabzilla.core.metadata.Metadata.StateId
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
        StateId(UUID.fromString(asJsonObject.getString("aggregateId"))),
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
      .put("aggregateId", eventMetadata.stateId.id.toString())
      .put("eventSequence", eventMetadata.eventSequence)
      .put("eventId", eventMetadata.eventId.id.toString())
      .put("causationId", eventMetadata.causationId.id.toString())
      .put("correlationId", eventMetadata.correlationId.id.toString())
  }
}
