package io.github.crabzilla.example1.customer

import io.github.crabzilla.context.JsonObjectSerDer
import io.vertx.core.json.JsonObject
import java.util.*

class CustomerEventSerDer : JsonObjectSerDer<CustomerEvent> {
  override fun fromJson(json: JsonObject): CustomerEvent {
    return when (val eventType = json.getString("type")) {
      "CustomerRegistered" ->
        CustomerEvent.CustomerRegistered(
          UUID.fromString(json.getString("id")),
          json.getString("name"),
        )
      "CustomerActivated" -> CustomerEvent.CustomerActivated(json.getString("reason"))
      "CustomerDeactivated" -> CustomerEvent.CustomerDeactivated(json.getString("reason"))
      "CustomerRenamed" -> CustomerEvent.CustomerRenamed(json.getString("name"))
      else -> throw IllegalArgumentException("Unknown event $eventType")
    }
  }

  override fun toJson(instance: CustomerEvent): JsonObject {
    val json = JsonObject().put("type", instance::class.simpleName)
    return when (instance) {
      is CustomerEvent.CustomerRegistered -> json.put("id", instance.id).put("name", instance.name)
      is CustomerEvent.CustomerActivated -> json.put("reason", instance.reason)
      is CustomerEvent.CustomerDeactivated -> json.put("reason", instance.reason)
      is CustomerEvent.CustomerRenamed -> json.put("name", instance.name)
    }
  }
}
