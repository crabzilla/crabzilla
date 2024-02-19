package io.github.crabzilla.example1.customer.serder

import io.github.crabzilla.context.JsonObjectSerDer
import io.github.crabzilla.example1.customer.model.CustomerCommand
import io.vertx.core.json.JsonObject
import java.util.*

class CustomerCommandSerDer : JsonObjectSerDer<CustomerCommand> {
  override fun toJson(instance: CustomerCommand): JsonObject {
    val json = JsonObject().put("type", instance::class.simpleName)
    return when (instance) {
      is CustomerCommand.RegisterCustomer ->
        json
          .put("id", instance.customerId)
          .put("name", instance.name)
      is CustomerCommand.ActivateCustomer ->
        json
          .put("reason", instance.reason)
      is CustomerCommand.DeactivateCustomer ->
        json
          .put("reason", instance.reason)
      is CustomerCommand.RegisterAndActivateCustomer ->
        json
          .put("id", instance.customerId)
          .put("name", instance.name)
          .put("reason", instance.reason)
      is CustomerCommand.RenameCustomer ->
        json
          .put("name", instance.name)
    }
  }

  override fun fromJson(json: JsonObject): CustomerCommand {
    return when (val commandType = json.getString("type")) {
      "RegisterCustomer" ->
        CustomerCommand.RegisterCustomer(
          UUID.fromString(json.getString("ID")),
          json.getString("name"),
        )
      "ActivateCustomer" -> CustomerCommand.ActivateCustomer(json.getString("reason"))
      "DeactivateCustomer" -> CustomerCommand.DeactivateCustomer(json.getString("reason"))
      "RenameCustomer" -> CustomerCommand.RenameCustomer(json.getString("name"))
      "RegisterAndActivateCustomer" ->
        CustomerCommand.RegisterAndActivateCustomer(
          UUID.fromString(json.getString("ID")),
          json.getString("name"),
          json.getString("reason"),
        )
      else -> throw IllegalArgumentException("Unknown command $commandType")
    }
  }
}
