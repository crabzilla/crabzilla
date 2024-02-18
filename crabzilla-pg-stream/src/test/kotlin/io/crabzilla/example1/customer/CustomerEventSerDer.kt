package io.crabzilla.example1.customer

import io.crabzilla.context.JsonObjectSerDer
import io.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.crabzilla.example1.customer.CustomerEvent.CustomerRenamed
import io.vertx.core.json.JsonObject
import java.util.*

class CustomerEventSerDer : JsonObjectSerDer<CustomerEvent> {
  override fun fromJson(json: JsonObject): CustomerEvent {
    return when (val eventType = json.getString("type")) {
      "CustomerRegistered" -> CustomerRegistered(UUID.fromString(json.getString("id")), json.getString("name"))
      "CustomerActivated" -> CustomerActivated(json.getString("reason"))
      "CustomerDeactivated" -> CustomerDeactivated(json.getString("reason"))
      "CustomerRenamed" -> CustomerRenamed(json.getString("name"))
      else -> throw IllegalArgumentException("Unknown event $eventType")
    }
  }

  override fun toJson(instance: CustomerEvent): JsonObject {
    val json = JsonObject().put("type", instance::class.simpleName)
    return when (instance) {
      is CustomerRegistered -> json.put("id", instance.id).put("name", instance.name)
      is CustomerActivated -> json.put("reason", instance.reason)
      is CustomerDeactivated -> json.put("reason", instance.reason)
      is CustomerRenamed -> json.put("name", instance.name)
    }
  }
}
