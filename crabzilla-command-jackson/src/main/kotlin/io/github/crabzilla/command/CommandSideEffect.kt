package io.github.crabzilla.command

import io.github.crabzilla.core.metadata.EventMetadata
import io.vertx.core.json.JsonObject

data class CommandSideEffect(
  val appendedEvents: List<Pair<JsonObject, EventMetadata>>,
  val resultingVersion: Int
) {
  fun causationId() = appendedEvents.last().second.eventId
  fun correlationId() = appendedEvents.last().second.correlationId
}
