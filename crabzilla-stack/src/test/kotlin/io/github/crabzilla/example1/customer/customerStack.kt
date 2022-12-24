package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.customer.CustomerCommand.*
import io.github.crabzilla.example1.customer.CustomerEvent.*
import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.JsonObjectSerDer
import io.github.crabzilla.stack.command.CommandServiceConfig
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import ulid4j.Ulid

val customerConfig = CommandServiceConfig(
  Customer::class,
  CustomerCommand::class,
  CustomerEvent::class,
  customerEventHandler,
  CustomerCommandHandler(),
  Customer.Initial
)

val ulidGenerator = Ulid()

val ulidFunction = { ulidGenerator.next() }

class CustomerJsonObjectSerDer: JsonObjectSerDer<Customer, CustomerCommand, CustomerEvent> {

  override fun eventFromJson(json: JsonObject): CustomerEvent {
    return when (val eventType = json.getString("type")) {
      "CustomerRegistered" ->
        CustomerRegistered(json.getString("id"), json.getString("name"))
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

  override fun commandFromJson(json: JsonObject): CustomerCommand {
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

class CustomersEventProjector : EventProjector {

  private val serDer = CustomerJsonObjectSerDer()

  override fun project(conn: SqlConnection, record: EventRecord): Future<Void> {
    val (payload, _, id) = record.extract()
    return when (val event = serDer.eventFromJson(payload)) {
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
