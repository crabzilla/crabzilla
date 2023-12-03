import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class EventMetadata(
  val stateType: String,
  val stateId: String,
  val eventId: String,
  val correlationId: String,
  val causationId: String,
  val eventSequence: Long,
  val version: Int,
  val eventType: String,
  // TODO app metadata
) {
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("stateType", this.stateType)
      .put("stateId", this.stateId)
      .put("eventSequence", this.eventSequence)
      .put("eventId", this.eventId)
      .put("causationId", this.causationId)
      .put("correlationId", this.correlationId)
      .put("version", this.version)
      .put("eventType", this.eventType)
  }
}

/**
 * An event record
 */
data class EventRecord(val metadata: EventMetadata, val payload: JsonObject) {
  companion object {
    fun List<EventRecord>.toJsonArray() = JsonArray(this.map { it.toJsonObject() })

    fun fromJsonObject(asJsonObject: JsonObject): EventRecord {
      val eventMetadata =
        EventMetadata(
          asJsonObject.getString("stateType"),
          asJsonObject.getString("stateId"),
          asJsonObject.getString("eventId"),
          asJsonObject.getString("correlationId"),
          asJsonObject.getString("causationId"),
          asJsonObject.getLong("eventSequence"),
          asJsonObject.getInteger("version"),
          asJsonObject.getString("eventType"),
        )
      return EventRecord(eventMetadata, asJsonObject.getJsonObject("eventPayload"))
    }
  }

  fun toJsonObject(): JsonObject {
    return metadata.toJsonObject()
      .put("eventPayload", payload)
  }

  fun extract() = Triple(payload, metadata, metadata.stateId)
}
