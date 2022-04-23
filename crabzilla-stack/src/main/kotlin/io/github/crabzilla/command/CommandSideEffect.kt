package io.github.crabzilla.command

import io.github.crabzilla.stack.EventRecord
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class CommandSideEffect(val appendedEvents: List<EventRecord>, val resultingVersion: Int) {
  fun latestEventId() = appendedEvents.last().metadata.eventId
  fun correlationId() = appendedEvents.last().metadata.correlationId
  fun jsonObject(): JsonObject = JsonObject().put("events", JsonArray(appendedEvents.map { it.toJsonObject() }))
}
