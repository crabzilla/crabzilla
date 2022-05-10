package io.github.crabzilla.command

import io.github.crabzilla.EventRecord
import io.vertx.core.json.JsonArray

data class CommandSideEffect(val appendedEvents: List<EventRecord>) {
  fun causationId() = appendedEvents.first().metadata.causationId
  fun latestEventId() = appendedEvents.last().metadata.eventId
  fun toJsonArray(): JsonArray = JsonArray(appendedEvents.map { it.toJsonObject() })
}
