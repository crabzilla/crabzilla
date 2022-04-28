package io.github.crabzilla.stack

import io.vertx.core.json.JsonArray

data class CommandSideEffect(val appendedEvents: List<EventRecord>) {
//  fun resultingVersion() = appendedEvents.last().metadata.version
  fun latestEventId() = appendedEvents.last().metadata.eventId
  fun correlationId() = appendedEvents.last().metadata.correlationId
  fun toJsonArray(): JsonArray = JsonArray(appendedEvents.map { it.toJsonObject() })
}
