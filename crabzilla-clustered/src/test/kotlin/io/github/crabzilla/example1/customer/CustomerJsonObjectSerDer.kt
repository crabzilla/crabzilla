package io.github.crabzilla.example1.customer

import io.github.crabzilla.JsonObjectSerDer
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.vertx.core.json.JsonObject
import java.util.UUID

class CustomerJsonObjectSerDer: JsonObjectSerDer<Customer, CustomerCommand, CustomerEvent> {

  override fun eventFromJson(json: JsonObject): CustomerEvent {
    return when (val eventType = json.getString("type")) {
      "CustomerRegistered" -> CustomerRegistered(UUID.fromString(json.getString("id")), json.getString("name"))
      "CustomerActivated" -> CustomerActivated(json.getString("reason"))
      "CustomerDeactivated" -> CustomerDeactivated(json.getString("reason"))
      else -> throw IllegalArgumentException("Unknown event $eventType")
    }
  }

  override fun eventToJson(event: CustomerEvent): JsonObject {
    return when (event) {
      is CustomerRegistered -> JsonObject()
        .put("type", event::class.simpleName)
        .put("id", event.id.toString())
        .put("name", event.name)
      is CustomerActivated ->
        JsonObject()
        .put("type", event::class.simpleName)
        .put("reason", event.reason)
      is CustomerDeactivated -> JsonObject()
        .put("type", event::class.simpleName)
        .put("reason", event.reason)
    }
  }

  override fun commandToJson(command: CustomerCommand): JsonObject {
    return when (command) {
      is RegisterCustomer -> JsonObject()
        .put("type", command::class.simpleName)
        .put("id", command.customerId.toString())
        .put("name", command.name)
      is ActivateCustomer -> JsonObject()
        .put("type", command::class.simpleName)
        .put("reason", command.reason)
      is DeactivateCustomer -> JsonObject()
        .put("type", command::class.simpleName)
        .put("reason", command.reason)
      is RegisterAndActivateCustomer -> JsonObject()
        .put("type", command::class.simpleName)
        .put("id", command.customerId.toString())
        .put("name", command.name)
        .put("reason", command.reason)
    }
  }
}