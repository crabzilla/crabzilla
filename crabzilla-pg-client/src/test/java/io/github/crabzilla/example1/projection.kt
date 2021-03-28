package io.github.crabzilla.example1

import io.github.crabzilla.core.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.EventRecord
import io.github.crabzilla.example1.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.CustomerEvent.CustomerRegistered
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * To update customer read model given events
 */
class CustomerReadModelProjectorVerticle(private val json: Json, private val repo: CustomerRepository) :
  AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(CustomerReadModelProjectorVerticle::class.java)
    private const val topic = "example1"
  }

  override fun start() {
    vertx.eventBus().consumer<JsonObject>(topic) { eventRecordAsJson ->
      val eventRecord = EventRecord.fromJsonObject(eventRecordAsJson.body())
      publish(eventRecord)
    }
    log.info("Started consuming from topic [$topic]")
  }

  override fun stop() {
    log.info("Stopped")
  }

  private fun publish(eventRecord: EventRecord): Future<Void> {
    log.info("Will project $eventRecord")
    val event = json.decodeFromString(DOMAIN_EVENT_SERIALIZER, eventRecord.eventAsjJson.toString()) as CustomerEvent
    log.info("The event is $event")
    return when (event) {
      is CustomerRegistered -> repo.upsert(eventRecord.aggregateId, event.name, false)
      is CustomerActivated -> repo.updateStatus(eventRecord.aggregateId, true)
      is CustomerDeactivated -> repo.updateStatus(eventRecord.aggregateId, false)
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
