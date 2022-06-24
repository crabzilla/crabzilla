package io.github.crabzilla.example1.customer

import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.JsonObjectSerDer
import io.github.crabzilla.stack.command.CommandServiceConfig
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

val customerConfig = CommandServiceConfig(
  Customer::class,
  CustomerCommand::class,
  CustomerEvent::class,
  customerEventHandler,
  CustomerCommandHandler()
)

class CustomerJsonObjectSerDer: JsonObjectSerDer<Customer, CustomerCommand, CustomerEvent> {

  override fun eventFromJson(json: JsonObject): CustomerEvent {
    return when (val eventType = json.getString("type")) {
      "CustomerRegistered" -> CustomerEvent.CustomerRegistered(UUID.fromString(json.getString("id")))
      "CustomerRegisteredPrivate" -> CustomerEvent.CustomerRegisteredPrivate(json.getString("name"))
      "CustomerActivated" -> CustomerEvent.CustomerActivated(json.getString("reason"))
      "CustomerDeactivated" -> CustomerEvent.CustomerDeactivated(json.getString("reason"))
      else -> throw IllegalArgumentException("Unknown event $eventType")
    }
  }

  override fun eventToJson(event: CustomerEvent): JsonObject {
    return when (event) {
      is CustomerEvent.CustomerRegistered -> JsonObject()
        .put("type", event::class.simpleName)
        .put("id", event.id.toString())
      is CustomerEvent.CustomerRegisteredPrivate -> JsonObject()
        .put("type", event::class.simpleName)
        .put("name", event.name)
      is CustomerEvent.CustomerActivated ->
        JsonObject()
          .put("type", event::class.simpleName)
          .put("reason", event.reason)
      is CustomerEvent.CustomerDeactivated -> JsonObject()
        .put("type", event::class.simpleName)
        .put("reason", event.reason)
    }
  }

  override fun commandToJson(command: CustomerCommand): JsonObject {
    return when (command) {
      is CustomerCommand.RegisterCustomer -> JsonObject()
        .put("type", command::class.simpleName)
        .put("id", command.customerId.toString())
        .put("name", command.name)
      is CustomerCommand.ActivateCustomer -> JsonObject()
        .put("type", command::class.simpleName)
        .put("reason", command.reason)
      is CustomerCommand.DeactivateCustomer -> JsonObject()
        .put("type", command::class.simpleName)
        .put("reason", command.reason)
      is CustomerCommand.RegisterAndActivateCustomer -> JsonObject()
        .put("type", command::class.simpleName)
        .put("id", command.customerId.toString())
        .put("name", command.name)
        .put("reason", command.reason)
    }
  }

  override fun commandFromJson(json: JsonObject): CustomerCommand {
    return when (val commandType = json.getString("type")) {
      "RegisterCustomer" -> CustomerCommand.RegisterCustomer(UUID.fromString(json.getString("ID")), json.getString("name"))
      "ActivateCustomer" -> CustomerCommand.ActivateCustomer(json.getString("reason"))
      "DeactivateCustomer" -> CustomerCommand.DeactivateCustomer(json.getString("reason"))
      "RegisterAndActivateCustomer" -> CustomerCommand.RegisterAndActivateCustomer(UUID.fromString(json.getString("ID")),
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
      is CustomerEvent.CustomerRegistered ->
        CustomersWriteDao.upsert(conn, id, false)
      is CustomerEvent.CustomerRegisteredPrivate ->
        CustomersWriteDao.updateName(conn, id, event.name)
      is CustomerEvent.CustomerActivated ->
        CustomersWriteDao.updateStatus(conn, id, true)
      is CustomerEvent.CustomerDeactivated ->
        CustomersWriteDao.updateStatus(conn, id, false)
    }
  }
}

object CustomersWriteDao {

  fun upsert(conn: SqlConnection, id: UUID, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("INSERT INTO customer_summary (id, is_active) VALUES ($1, $2)")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }

  fun updateName(conn: SqlConnection, id: UUID, name: String): Future<Void> {
    return conn
      .preparedQuery("UPDATE customer_summary set name = $2 WHERE id = $1")
      .execute(Tuple.of(id, name))
      .mapEmpty()
  }

  fun updateStatus(conn: SqlConnection, id: UUID, isActive: Boolean): Future<Void> {
    return conn
      .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }
}
