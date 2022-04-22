package io.github.crabzilla.command

import io.github.crabzilla.core.metadata.EventMetadata
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class CommandSideEffect(
  val appendedEvents: List<Pair<JsonObject, EventMetadata>>,
  val resultingVersion: Int
) {
  fun causationId() = appendedEvents.last().second.eventId
  fun correlationId() = appendedEvents.last().second.correlationId
  fun toJson(): JsonObject {
    fun pairAsJson(pair: Pair<JsonObject, EventMetadata>): JsonObject {
      val (eventAsjJson, eventMetadata) = pair
      return JsonObject()
        .put("eventAsjJson", eventAsjJson)
        .put("aggregateName", eventMetadata.stateType)
        .put("aggregateId", eventMetadata.stateId.toString())
        .put("eventSequence", eventMetadata.eventSequence)
        .put("eventId", eventMetadata.eventId.toString())
        .put("causationId", eventMetadata.causationId.toString())
        .put("correlationId", eventMetadata.correlationId.toString())
    }
    return JsonObject()
      .put("events", JsonArray(appendedEvents.map { pairAsJson(it) }))
  }
}
