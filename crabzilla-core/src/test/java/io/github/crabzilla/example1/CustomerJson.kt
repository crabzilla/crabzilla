package io.github.crabzilla.example1

import io.github.crabzilla.Command
import io.github.crabzilla.DomainEvent
import io.vertx.core.json.JsonObject

object CustomerJson {

  val CUSTOMER_FROM_JSON = { json: JsonObject ->
    Customer(CustomerId(json.getInteger("customerId")), json.getString("name"), json.getBoolean("isActive"),
      json.getString("reason"))
  }

  val CUSTOMER_TO_JSON = { customer: Customer ->
    JsonObject().put("customerId", customer.customerId?.value).put("name", customer.name)
      .put("isActive", customer.isActive).put("reason", customer.reason)
  }

  val CUSTOMER_EVENT_FROM_JSON = { eventName: String, jo: JsonObject ->
    when (eventName) {
      "CustomerCreated" -> CustomerCreated(CustomerId(jo.getJsonObject("customerId").getInteger("value")),
        jo.getString("name"))
      "CustomerActivated" -> CustomerActivated(jo.getString("reason"), jo.getInstant("_when"))
      "CustomerDeactivated" -> CustomerDeactivated(jo.getString("reason"), jo.getInstant("_when"))
      else -> throw java.lang.IllegalArgumentException("$eventName is unknown")
    }
  }

  val CUSTOMER_EVENT_TO_JSON = { event: DomainEvent ->
    when (event) {
      is CustomerCreated -> JsonObject.mapFrom(event)
      is CustomerActivated -> JsonObject.mapFrom(event)
      is CustomerDeactivated -> JsonObject.mapFrom(event)
      else -> throw java.lang.IllegalArgumentException("$event is unknown")
    }
  }

  val CUSTOMER_CMD_FROM_JSON = { cmdName: String, jo: JsonObject ->
    when (cmdName) {
      "create" -> CreateCustomer(jo.getString("name"))
      "activate" -> ActivateCustomer(jo.getString("reason"))
      "deactivate" -> DeactivateCustomer(jo.getString("reason"))
      "create-activate" -> CreateActivateCustomer(jo.getString("name"), jo.getString("reason"))
      else -> throw IllegalArgumentException("$cmdName is unknown")
    }
  }

  val CUSTOMER_CMD_TO_JSON = { cmd: Command ->
    when (cmd) {
      is CreateCustomer -> JsonObject.mapFrom(cmd)
      is ActivateCustomer -> JsonObject.mapFrom(cmd)
      is DeactivateCustomer -> JsonObject.mapFrom(cmd)
      is CreateActivateCustomer -> JsonObject.mapFrom(cmd)
      else -> throw IllegalArgumentException("$cmd is unknown")
    }
  }

}
