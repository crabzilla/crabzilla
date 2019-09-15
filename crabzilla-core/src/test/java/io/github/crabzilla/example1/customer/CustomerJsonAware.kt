package io.github.crabzilla.example1.customer

import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.EntityJsonAware
import io.vertx.core.json.JsonObject

class CustomerJsonAware : EntityJsonAware<Customer> {

  override fun toJson(entity: Customer): JsonObject {
    return JsonObject().put("customerId", entity.customerId).put("name", entity.name)
      .put("isActive", entity.isActive).put("reason", entity.reason)
  }

  override fun fromJson(json: JsonObject): Customer {
    return Customer(json.getInteger("customerId"), json.getString("name"), json.getBoolean("isActive"),
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

  override fun eventFromJson(eventName: String, json: JsonObject): Pair<String, DomainEvent> {
    return when (eventName) {
      "CustomerCreated" -> Pair(eventName, CustomerCreated(json.getInteger("customerId"), json.getString("name")))
      "CustomerActivated" -> Pair(eventName, CustomerActivated(json.getString("reason"), json.getInstant("_when")))
      "CustomerDeactivated" -> Pair(eventName, CustomerDeactivated(json.getString("reason"), json.getInstant("_when")))
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
