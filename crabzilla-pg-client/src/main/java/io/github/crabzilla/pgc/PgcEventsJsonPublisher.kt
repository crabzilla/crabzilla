package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRootName
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
 * 1) Read model
 * 2) Downstream services. In this case the EventPublisher implementation should apply a function to export
 * an Integration Event instead. And also transform it into plain JSON. The publisher and consumers should agree
 * on a JSON schema to reduce libraries coupling and the versioning overhead.
 */
class PgcEventsJsonPublisher(
  private val jsonEventPublisher: JsonEventPublisher,
  private val aggregateRootName: AggregateRootName,
  private val writeModelDb: PgPool
) {

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventsJsonPublisher::class.java)
    private const val SELECT_EVENTS_VERSION_AFTER_VERSION =
      "SELECT event_payload, ar_id, event_id FROM events " +
        "WHERE ar_name = $1 and event_id > $2 " +
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
        .query("LISTEN ${aggregateRootName.value}")
        .execute { ar -> log.info("Subscribed to channel ${aggregateRootName.value} $ar") }
      pgConn.notificationHandler {
        log.info("Received a notification #${notifications.incrementAndGet()} from channel ${it.channel}")
        scan()
      }
    }
  }

  fun scan(): Future<Void> {

    fun scanForNewEvents(conn: SqlConnection): Future<List<Triple<Int, JsonObject, Long>>> {
      val promise = Promise.promise<List<Triple<Int, JsonObject, Long>>>()
      val events = mutableListOf<Triple<Int, JsonObject, Long>>()
      conn.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION)
        .onFailure { promise.fail(it) }
        .onSuccess { preparedStatement ->
          // Streams require to run within a transaction
          conn.begin()
            .onFailure {
              promise.fail(it)
            }
            .onSuccess { tx ->
              // Fetch ROWS_PER_TIME
              val stream = preparedStatement.createStream(ROWS_PER_TIME, Tuple.of(aggregateRootName.value, lastEventId))
              // Use the stream
              stream.exceptionHandler { err -> log.error("Stream error", err) }
              stream.handler { row ->
                val jsonObject: JsonObject = row.get(JsonObject::class.java, 0)
                val triple = Triple(row.getInteger(1), jsonObject, row.getLong(2))
                events.add(triple)
                if (log.isDebugEnabled) {
                  log.debug("$triple")
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

    log.info("Will scan ${aggregateRootName.value} events since $lastEventId")

    val promise = Promise.promise<Void>()
    writeModelDb.connection
      .onFailure { promise.fail(it) }
      .onSuccess { conn: SqlConnection ->
        scanForNewEvents(conn)
          .onFailure {
            promise.fail(it)
          }
          .onSuccess { listOfTriple ->
            val futures: List<Future<Void>> = listOfTriple.map { jsonEventPublisher.publish(it.third, it.first, it.second) }
            log.info("Found $listOfTriple")
            CompositeFuture.join(futures) // TODO fix it to support more than 6 events - using fold or kotlin continuation
              .onFailure {
                promise.fail(it)
              }
              .onSuccess {
                log.info("Events successfully published")
                if (listOfTriple.isNotEmpty()) {
                  lastEventId.set(listOfTriple.last().third)
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
