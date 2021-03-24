package io.github.crabzilla.pgc

import io.github.crabzilla.core.EventRecord
import io.github.crabzilla.core.JsonEventPublisher
import io.github.crabzilla.pgc.PgcClient.close
import io.github.crabzilla.pgc.PgcClient.commit
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnection
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * This component will be triggered using a Postgres LISTEN command. Then it can publish the domain events to
 * a JsonEventPublisher (may be a outbox publisher to any broker).
 */
class PgcOutboxPublisher(
  private val topic: String,
  private val jsonEventPublisher: JsonEventPublisher,
  private val writeModelDb: PgPool
) {

  companion object {
    private val log = LoggerFactory.getLogger(PgcOutboxPublisher::class.java)
    private const val SELECT_EVENTS_VERSION_AFTER_VERSION =
      "SELECT ar_name, ar_id, event_payload, event_id " +
        "FROM events " +
        "WHERE event_id > $1 " +
        "ORDER BY event_id " +
        "LIMIT 6 " // TODO fix it to support more than 6 events per scan
    private const val ROWS_PER_TIME = 1000
  }

  private val notifications = AtomicInteger()
  private val lastEventId = AtomicLong() // TODO persist this offset into a control table
  private lateinit var pgConn: PgConnection

  init {
    writeModelDb.getConnection { c: AsyncResult<SqlConnection> ->
      pgConn = c.result() as PgConnection
      pgConn
        .query("LISTEN $topic")
        .execute { ar -> log.info("Subscribed to channel $topic ${ar.result()}") }
      pgConn.notificationHandler {
        log.info("Received a notification #${notifications.incrementAndGet()} from channel ${it.channel}")
        scan()
      }
    }
  }

  fun scan(): Future<Void> {

    fun scanForNewEvents(conn: SqlConnection): Future<List<EventRecord>> {
      val promise = Promise.promise<List<EventRecord>>()
      val events = mutableListOf<EventRecord>()
      conn.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION)
        .onFailure { promise.fail(it) }
        .onSuccess { preparedStatement ->
          // Streams require to run within a transaction
          conn.begin()
            .onFailure { promise.fail(it) }
            .onSuccess { tx ->
              // Fetch ROWS_PER_TIME
              val stream = preparedStatement.createStream(ROWS_PER_TIME, Tuple.of(lastEventId))
              // Use the stream
              stream.exceptionHandler { err -> log.error("Stream error", err) }
              stream.handler { row ->
                log.info(row.deepToString())
                val jsonObject: JsonObject = row.get(JsonObject::class.java, 2)
                val record = EventRecord(row.getString(0), row.getInteger(1), jsonObject, row.getLong(3))
                events.add(record)
                if (log.isDebugEnabled) {
                  log.debug("$record")
                }
              }
              stream.endHandler {
                if (log.isDebugEnabled) log.debug("End of stream")
                commit(tx)
                  .onFailure { promise.fail(it) }
                  .onSuccess { promise.complete(events) }
              }
            }
        }
      return promise.future()
    }

    log.info("Will scan $topic events since $lastEventId")

    val promise = Promise.promise<Void>()
    writeModelDb.connection
      .onFailure { promise.fail(it) }
      .onSuccess { conn: SqlConnection ->
        scanForNewEvents(conn)
          .onFailure { promise.fail(it) }
          .onSuccess { eventsList ->
            val futures: List<Future<Void>> = eventsList
              .map { jsonEventPublisher.publish(it) }
            log.info("Found $eventsList")
            CompositeFuture.join(futures) // TODO fix it to support more than 6 events - using fold or kotlin continuation
              .onFailure { promise.fail(it) }
              .onSuccess {
                log.info("Events successfully published")
                if (eventsList.isNotEmpty()) {
                  lastEventId.set(eventsList.last().eventId)
                }
                promise.complete()
              }
          }.onComplete {
            close(conn)
          }
      }
    return promise.future()
  }
}
