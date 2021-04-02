package io.github.crabzilla.stack

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf

/**
 * An event record
 */
data class EventRecord(val aggregateName: String, val aggregateId: Int, val eventAsjJson: JsonObject, val eventId: Long) {
  companion object {
    fun fromJsonObject(asJsonObject: JsonObject): EventRecord {
      return EventRecord(
        asJsonObject.getString("aggregateName"),
        asJsonObject.getInteger("aggregateId"),
        asJsonObject.getJsonObject("eventAsjJson"),
        asJsonObject.getLong("eventId")
      )
    }
  }
  fun toJsonObject(): JsonObject {
    return jsonObjectOf(
      Pair("aggregateName", aggregateName),
      Pair("aggregateId", aggregateId),
      Pair("eventAsjJson", eventAsjJson),
      Pair("eventId", eventId)
    )
  }
}
