package io.github.crabzilla.example1.customer

import EventProjector
import EventRecord
import JsonObjectSerDer
import io.github.crabzilla.command.CommandComponentConfig
import io.github.crabzilla.example1.customer.CustomerCommand.*
import io.github.crabzilla.example1.customer.CustomerEvent.*
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

val customerConfig = CommandComponentConfig(
  stateClass = Customer::class,
  commandSerDer = CustomerSerDer(),
  eventSerDer = CustomerEventsSerDer(),
  eventHandler = customerEventHandler,
  commandHandler = customerCommandHandler,
  initialState = Customer.Initial
)

class CustomerSerDer: JsonObjectSerDer<CustomerCommand> {

  override fun toJson(instance: CustomerCommand): JsonObject {
    val json = JsonObject().put("type", instance::class.simpleName)
    return when (instance) {
      is RegisterCustomer -> json
        .put("id", instance.customerId)
        .put("name", instance.name)
      is ActivateCustomer -> json
        .put("reason", instance.reason)
      is DeactivateCustomer -> json
        .put("reason", instance.reason)
      is RegisterAndActivateCustomer -> json
        .put("id", instance.customerId)
        .put("name", instance.name)
        .put("reason", instance.reason)
    }
  }

  override fun fromJson(json: JsonObject): CustomerCommand {
    return when (val commandType = json.getString("type")) {
      "RegisterCustomer" -> RegisterCustomer(json.getString("ID"), json.getString("name"))
      "ActivateCustomer" -> ActivateCustomer(json.getString("reason"))
      "DeactivateCustomer" -> DeactivateCustomer(json.getString("reason"))
      "RegisterAndActivateCustomer" -> RegisterAndActivateCustomer(json.getString("ID"),
        json.getString("name"), json.getString("reason"))
      else -> throw IllegalArgumentException("Unknown command $commandType")
    }
  }
}

class CustomerEventsSerDer: JsonObjectSerDer<CustomerEvent> {

  override fun fromJson(json: JsonObject): CustomerEvent {
    return when (val eventType = json.getString("type")) {
      "CustomerRegistered" -> CustomerRegistered(json.getString("id"), json.getString("name"))
      "CustomerActivated" -> CustomerActivated(json.getString("reason"))
      "CustomerDeactivated" -> CustomerDeactivated(json.getString("reason"))
      else -> throw IllegalArgumentException("Unknown event $eventType")
    }
  }

  override fun toJson(instance: CustomerEvent): JsonObject {
    val json = JsonObject().put("type", instance::class.simpleName)
    return when (instance) {
      is CustomerRegistered -> json.put("id", instance.id).put("name", instance.name)
      is CustomerActivated -> json.put("reason", instance.reason)
      is CustomerDeactivated -> json.put("reason", instance.reason)
    }
  }

}

class CustomersEventProjector : EventProjector {

  private val serDer = CustomerEventsSerDer()

  override fun project(conn: SqlConnection, eventRecord: EventRecord): Future<Void> {
    val (payload, _, id) = eventRecord.extract()
    return when (val event = serDer.fromJson(payload)) {
      is CustomerRegistered ->
        CustomersWriteDao.upsert(conn, id,  event.name, false)
      is CustomerActivated ->
        CustomersWriteDao.updateStatus(conn, id, true)
      is CustomerDeactivated ->
        CustomersWriteDao.updateStatus(conn, id, false)
    }
  }
}

object CustomersWriteDao {

  fun upsert(conn: SqlConnection, id: String, name: String, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)")
      .execute(Tuple.of(id, name, isActive))
      .mapEmpty()
  }

  fun updateStatus(conn: SqlConnection, id: String, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }
}
