package io.github.crabzilla.context

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.*

data class TargetStream(
  private val stateType: String? = null,
  private val stateId: String? = null,
  val name: String = "$stateType@$stateId",
  val mustBeNew: Boolean = false,
) {
  fun stateType(): String {
    return stateType ?: name.split("@")[0]
  }

  fun stateId(): String {
    return stateId ?: name.split("@")[1]
  }
}

data class EventMetadata(
  val streamId: Int,
  val stateType: String?,
  val stateId: String?,
  val eventId: UUID,
  val correlationId: UUID,
  val causationId: UUID,
  val eventSequence: Long,
  val version: Int,
  val eventType: String,
) {
  fun toJsonObject(): JsonObject {
    return JsonObject()
      .put("streamId", this.streamId)
      .put("stateType", this.stateType)
      .put("stateId", this.stateId)
      .put("eventSequence", this.eventSequence)
      .put("eventId", this.eventId.toString())
      .put("causationId", this.causationId.toString())
      .put("correlationId", this.correlationId.toString())
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
          streamId = asJsonObject.getInteger("streamId"),
          stateType = asJsonObject.getString("stateType"),
          stateId = asJsonObject.getString("stateId"),
          eventId = UUID.fromString(asJsonObject.getString("eventId")),
          correlationId = UUID.fromString(asJsonObject.getString("correlationId")),
          causationId = UUID.fromString(asJsonObject.getString("causationId")),
          eventSequence = asJsonObject.getLong("eventSequence"),
          version = asJsonObject.getInteger("version"),
          eventType = asJsonObject.getString("eventType"),
        )
      return EventRecord(eventMetadata, asJsonObject.getJsonObject("eventPayload"))
    }
  }

  fun toJsonObject(): JsonObject {
    return metadata.toJsonObject()
      .put("eventPayload", payload)
  }

  fun extract() = Pair(payload, metadata)
}
