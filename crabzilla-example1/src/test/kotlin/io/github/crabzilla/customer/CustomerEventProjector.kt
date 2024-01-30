package io.github.crabzilla.customer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.crabzilla.context.EventProjector
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

class CustomerEventProjector : EventProjector {
  private val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT)
  private val serDer = JacksonJsonObjectSerDer(json, clazz = CustomerEvent::class)

  override fun project(
    conn: SqlConnection,
    eventRecord: EventRecord,
  ): Future<Void> {
    val (payload, _, id) = eventRecord.extract()
    return when (val event = serDer.fromJson(payload)) {
      is CustomerRegistered ->
        insert(conn, id!!, event.name)
      is CustomerActivated ->
        updateStatus(conn, id!!, true)
      is CustomerDeactivated ->
        updateStatus(conn, id!!, false)
      is CustomerEvent.CustomerRenamed -> TODO()
    }
  }

  private fun insert(
    conn: SqlConnection,
    id: String,
    name: String,
  ): Future<Void> {
    return conn
      .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)")
      .execute(Tuple.of(id, name, false))
      .mapEmpty()
  }

  private fun updateStatus(
    conn: SqlConnection,
    id: String,
    isActive: Boolean,
  ): Future<Void> {
    return conn
      .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
      .execute(Tuple.of(id, isActive))
      .mapEmpty()
  }
}
