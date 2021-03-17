package io.github.crabzilla.pgc

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EventPublisher
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnection
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
class PgcEventsPublisher<E : DomainEvent>(
  private val eventPublisher: EventPublisher<E>,
  private val aggregateRootName: String,
  private val writeModelDb: PgPool,
  private val json: Json
) {

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventsPublisher::class.java)
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
        .query("LISTEN $aggregateRootName")
        .execute { ar -> log.info("Subscribed to channel $aggregateRootName $ar") }
      pgConn.notificationHandler {
        log.info("Received a notification #${notifications.incrementAndGet()} from channel ${it.channel}")
        scan()
      }
    }
  }

  fun scan(): Future<Void> {

    fun scanForNewEvents(conn: SqlConnection): Future<List<Triple<Int, E, Long>>> {
      val promise = Promise.promise<List<Triple<Int, E, Long>>>()
      val events = mutableListOf<Triple<Int, E, Long>>()
      conn.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION) { ar0 ->
        if (ar0.failed()) {
          promise.fail(ar0.cause())
          return@prepare
        }
        val pq: PreparedStatement = ar0.result()
        // Streams require to run within a transaction
        conn.begin { ar1 ->
          if (ar1.failed()) {
            promise.fail(ar1.cause())
            return@begin
          } else {
            val tx: Transaction = ar1.result()
            // Fetch ROWS_PER_TIME
            val stream = pq.createStream(ROWS_PER_TIME, Tuple.of(aggregateRootName, lastEventId))
            // Use the stream
            stream.exceptionHandler { err -> log.error("Stream error", err) }
            stream.handler { row ->
              val jsonObject: JsonObject = row.get(JsonObject::class.java, 0)
              val event: DomainEvent = json.decodeFromString(jsonObject.encode())
              val triple = Triple(row.getInteger(1), event as E, row.getLong(2))
              events.add(triple)
              if (log.isDebugEnabled) {
                log.debug("$triple")
              }
            }
            stream.endHandler {
              if (log.isDebugEnabled) log.debug("End of stream")
              // Attempt to commit the transaction
              tx.commit { ar ->
                if (ar.failed()) {
                  log.error("tx.commit", ar.cause())
                  promise.fail(ar.cause())
                } else {
                  if (log.isDebugEnabled) log.debug("tx.commit successfully")
                  promise.complete(events)
                }
              }
            }
          }
        }
      }
      return promise.future()
    }

    log.info("Will scan $aggregateRootName events since $lastEventId")

    val promise = Promise.promise<Void>()
    writeModelDb.connection
      .compose { conn: SqlConnection -> scanForNewEvents(conn) }
      .onFailure {
        log.error("When pulling new events", it.cause)
        promise.fail(it.cause)
      }
      .onSuccess { listOfTriple ->
        val futures: List<Future<Void>> = listOfTriple.map { eventPublisher.project(it.first, it.second) }
        log.info("Found $listOfTriple")
        CompositeFuture.join(futures) // TODO fix it to support more than 6 events - using fold or kotlin continuation
          .onFailure {
            log.error("Error: ", it.cause)
            promise.fail(it.cause)
          }
          .onSuccess {
            log.info("Fine")
            if (listOfTriple.isNotEmpty()) {
              lastEventId.set(listOfTriple.last().third)
            }
            promise.complete()
          }
      }
    return promise.future()
  }
}
