package io.github.crabzilla.example1.customer

import io.github.crabzilla.context.EventProjector
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.JsonObjectSerDer
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RenameCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRenamed
import io.github.crabzilla.writer.CrabzillaWriterConfig
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

val customerConfig =
  CrabzillaWriterConfig(
    initialState = Customer.Initial,
    eventHandler = customerEventHandler,
    commandHandler = customerCommandHandler,
    eventSerDer = CustomerEventSerDer(),
    commandSerDer = CustomerCommandSerDer(),
  )

class CustomerCommandSerDer : JsonObjectSerDer<CustomerCommand> {
  override fun toJson(instance: CustomerCommand): JsonObject {
    val json = JsonObject().put("type", instance::class.simpleName)
    return when (instance) {
      is RegisterCustomer ->
        json
          .put("id", instance.customerId)
          .put("name", instance.name)
      is ActivateCustomer ->
        json
          .put("reason", instance.reason)
      is DeactivateCustomer ->
        json
          .put("reason", instance.reason)
      is RegisterAndActivateCustomer ->
        json
          .put("id", instance.customerId)
          .put("name", instance.name)
          .put("reason", instance.reason)
      is RenameCustomer ->
        json
          .put("name", instance.name)
    }
  }

  override fun fromJson(json: JsonObject): CustomerCommand {
    return when (val commandType = json.getString("type")) {
      "RegisterCustomer" -> RegisterCustomer(UUID.fromString(json.getString("ID")), json.getString("name"))
      "ActivateCustomer" -> ActivateCustomer(json.getString("reason"))
      "DeactivateCustomer" -> DeactivateCustomer(json.getString("reason"))
      "RenameCustomer" -> RenameCustomer(json.getString("name"))
      "RegisterAndActivateCustomer" ->
        RegisterAndActivateCustomer(
          UUID.fromString(json.getString("ID")),
          json.getString("name"),
          json.getString("reason"),
        )
      else -> throw IllegalArgumentException("Unknown command $commandType")
    }
  }
}

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

class CustomersEventProjector : EventProjector {
  private val serDer = CustomerEventSerDer()

  override fun project(
    conn: SqlConnection,
    eventRecord: EventRecord,
  ): Future<Void> {
    val (payload, _, idAsString) = eventRecord.extract()
    val idAsUUID = UUID.fromString(idAsString)
    return when (val event = serDer.fromJson(payload)) {
      is CustomerRegistered ->
        conn
          .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)")
          .execute(Tuple.of(idAsUUID, event.name, false))
          .mapEmpty()
      is CustomerActivated ->
        updateStatus(conn, idAsUUID, true)
      is CustomerDeactivated ->
        updateStatus(conn, idAsUUID, false)
      is CustomerRenamed -> TODO()
    }
  }

  companion object {
    fun updateStatus(
      conn: SqlConnection,
      id: UUID,
      isActive: Boolean,
    ): Future<Void> {
      return conn
        .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
        .execute(Tuple.of(id, isActive))
        .mapEmpty()
    }
  }
}