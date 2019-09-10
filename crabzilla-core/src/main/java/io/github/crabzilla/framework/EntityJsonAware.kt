package io.github.crabzilla.framework

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

interface EntityJsonAware<E: Entity> {

  fun fromJson(json: JsonObject): E

  fun toJson(entity: E): JsonObject

  fun cmdFromJson(cmdName: String, json: JsonObject): Command

  fun cmdToJson(cmd: Command): JsonObject

  fun eventFromJson(eventName: String, json: JsonObject): Pair<String, DomainEvent>

  fun eventToJson(event: DomainEvent): JsonObject

  fun toJsonArray(events: List<Pair<String, DomainEvent>>): JsonArray {
    val eventsJsonArray = JsonArray()
    events
      .map { pair -> JsonObject().put(UnitOfWork.JsonMetadata.EVENT_NAME, pair.first)
                                 .put(UnitOfWork.JsonMetadata.EVENTS_JSON_CONTENT, eventToJson(pair.second))}
      .forEach { jo -> eventsJsonArray.add(jo) }
    return eventsJsonArray
  }

}
