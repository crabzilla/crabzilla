package io.github.crabzilla.example1

import io.github.crabzilla.Command
import io.github.crabzilla.DomainEvent
import io.github.crabzilla.EntityJsonSerDer
import io.vertx.core.json.JsonObject

class CustomerJson : EntityJsonSerDer<Customer> {

  override fun toJson(customer: Customer): JsonObject {
    return JsonObject().put("customerId", customer.customerId?.value).put("name", customer.name)
      .put("isActive", customer.isActive).put("reason", customer.reason)
  }

  override fun fromJson(json: JsonObject): Customer {
    return Customer(CustomerId(json.getInteger("customerId")), json.getString("name"), json.getBoolean("isActive"),
      json.getString("reason"))
  }

  override fun cmdFromJson(cmdName: String, json: JsonObject): Command {
    return when (cmdName) {
      "create" -> CreateCustomer(json.getString("name"))
      "activate" -> ActivateCustomer(json.getString("reason"))
      "deactivate" -> DeactivateCustomer(json.getString("reason"))
      "create-activate" -> CreateActivateCustomer(json.getString("name"), json.getString("reason"))
      else -> throw IllegalArgumentException("$cmdName is unknown")
    }
  }

  override fun cmdToJson(cmd: Command): JsonObject {
    return when (cmd) {
      is CreateCustomer -> JsonObject.mapFrom(cmd)
      is ActivateCustomer -> JsonObject.mapFrom(cmd)
      is DeactivateCustomer -> JsonObject.mapFrom(cmd)
      is CreateActivateCustomer -> JsonObject.mapFrom(cmd)
      else -> throw IllegalArgumentException("$cmd is unknown")
    }
  }

  override fun eventFromJson(eventName: String, jo: JsonObject): Pair<String, DomainEvent> {
    return when (eventName) {
      "CustomerCreated" -> Pair(eventName, CustomerCreated(
        CustomerId(jo.getJsonObject("customerId").getInteger("value")), jo.getString("name")))
      "CustomerActivated" -> Pair(eventName, CustomerActivated(jo.getString("reason"), jo.getInstant("_when")))
      "CustomerDeactivated" -> Pair(eventName, CustomerDeactivated(jo.getString("reason"), jo.getInstant("_when")))
      else -> throw java.lang.IllegalArgumentException("$eventName is unknown")
    }
  }

  override fun eventToJson(event: DomainEvent): JsonObject {
    return when (event) {
      is CustomerCreated -> JsonObject.mapFrom(event)
      is CustomerActivated -> JsonObject.mapFrom(event)
      is CustomerDeactivated -> JsonObject.mapFrom(event)
      else -> throw java.lang.IllegalArgumentException("$event is unknown")
    }
  }

}
