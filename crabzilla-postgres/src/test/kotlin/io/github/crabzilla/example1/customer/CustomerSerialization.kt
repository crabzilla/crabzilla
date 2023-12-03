import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.vertx.core.json.JsonObject

class CustomerSerDer : JsonObjectSerDer<CustomerCommand> {
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
    }
  }

  override fun fromJson(json: JsonObject): CustomerCommand {
    return when (val commandType = json.getString("type")) {
      "RegisterCustomer" -> CustomerCommand.RegisterCustomer(json.getString("ID"), json.getString("name"))
      "ActivateCustomer" -> CustomerCommand.ActivateCustomer(json.getString("reason"))
      "DeactivateCustomer" -> CustomerCommand.DeactivateCustomer(json.getString("reason"))
      "RegisterAndActivateCustomer" ->
        CustomerCommand.RegisterAndActivateCustomer(
          json.getString("ID"),
          json.getString("name"),
          json.getString("reason"),
        )
      else -> throw IllegalArgumentException("Unknown command $commandType")
    }
  }
}

class CustomerEventsSerDer : JsonObjectSerDer<CustomerEvent> {
  override fun fromJson(json: JsonObject): CustomerEvent {
    return when (val eventType = json.getString("type")) {
      "CustomerRegistered" -> CustomerEvent.CustomerRegistered(json.getString("id"), json.getString("name"))
      "CustomerActivated" -> CustomerEvent.CustomerActivated(json.getString("reason"))
      "CustomerDeactivated" -> CustomerEvent.CustomerDeactivated(json.getString("reason"))
      else -> throw IllegalArgumentException("Unknown event $eventType")
    }
  }

  override fun toJson(instance: CustomerEvent): JsonObject {
    val json = JsonObject().put("type", instance::class.simpleName)
    return when (instance) {
      is CustomerEvent.CustomerRegistered -> json.put("id", instance.id).put("name", instance.name)
      is CustomerEvent.CustomerActivated -> json.put("reason", instance.reason)
      is CustomerEvent.CustomerDeactivated -> json.put("reason", instance.reason)
    }
  }
}
