package io.github.crabzilla.example1

import io.github.crabzilla.core.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.JsonEventPublisher
import io.github.crabzilla.example1.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.CustomerEvent.CustomerRegistered
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.json.Json

/**
 * To update customer read model given events
 */
class CustomerReadModelProjector(private val json: Json, private val repo: CustomerRepository) : JsonEventPublisher {

  override fun publish(eventId: Long, id: Int, eventAsJson: JsonObject): Future<Void> {
    return when (val event = json.decodeFromString(DOMAIN_EVENT_SERIALIZER, eventAsJson.toString()) as CustomerEvent) {
      is CustomerRegistered -> repo.upsert(id, event.name, false)
      is CustomerActivated -> repo.updateStatus(id, true)
      is CustomerDeactivated -> repo.updateStatus(id, false)
    }
  }
}

/**
 * Read model repository
 */
class CustomerRepository(private val pool: PgPool) {
  fun upsert(id: Int, name: String, isActive: Boolean): Future<Void> {
    val promise = Promise.promise<Void>()
    pool
      .preparedQuery(
        "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3) ON CONFLICT (id) DO UPDATE " +
          "SET name = $2, is_active = $3"
      )
      .execute(Tuple.of(id, name, isActive)) { ar ->
        if (ar.succeeded()) {
          val rows: RowSet<Row> = ar.result()
          println("Got " + rows.size().toString() + " rows ")
          promise.complete()
        } else {
          println("Failure: " + ar.cause().message)
          promise.fail(ar.cause().message)
        }
      }
    return promise.future()
  }

  fun updateStatus(id: Int, isActive: Boolean): Future<Void> {
    val promise = Promise.promise<Void>()
    pool
      .preparedQuery("UPDATE customer_summary set is_active = $2 where id = $1")
      .execute(Tuple.of(id)) { ar ->
        if (ar.succeeded()) {
          val rows: RowSet<Row> = ar.result()
          println("Got " + rows.size().toString() + " rows ")
          promise.complete()
        } else {
          println("Failure: " + ar.cause().message)
          promise.fail(ar.cause().message)
        }
      }
    return promise.future()
  }
}
