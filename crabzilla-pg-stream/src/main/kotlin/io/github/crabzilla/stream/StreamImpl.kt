package io.github.crabzilla.stream

import io.github.crabzilla.context.CrabzillaWriterException
import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.JsonObjectSerDer
import io.github.crabzilla.context.TargetStream
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.RowStream
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.*

class StreamRepositoryImpl<S : Any, E : Any>(
  private val conn: SqlConnection,
  private val targetStream: TargetStream,
  private val initialState: S,
  private val eventSerDer: JsonObjectSerDer<E>,
  private val eventHandler: (S, E) -> S,
) : StreamRepository<S> {
  private val log = LoggerFactory.getLogger("${StreamRepositoryImpl::class.java.simpleName}-${targetStream.stateType}")

  override fun getStreamId(): Future<Int> {
    val params = Tuple.of(targetStream.name)
    log.debug("Will get stream {}", targetStream.name)
    return conn.preparedQuery(StreamWriterImpl.SQL_GET_STREAM)
      .execute(params)
      .map {
        if (it.rowCount() == 0) {
          NO_STREAM
        } else {
          it.first().getInteger("id")
        }
      }
  }

  override fun getSnapshot(streamId: Int): Future<StreamSnapshot<S>> {
    val promise = Promise.promise<StreamSnapshot<S>>()
    return conn
      .prepare(SQL_GET_EVENTS_BY_STREAM_ID)
      .compose { pq: PreparedStatement ->
        var state: S = initialState
        var latestVersion = 0
        var lastCausationId: UUID? = null
        var lastCorrelationId: UUID? = null
        var error: Throwable? = null
        // Fetch QUERY_MAX_STREAM_SIZE rows at a time
        val stream: RowStream<Row> = pq.createStream(QUERY_MAX_STREAM_SIZE, Tuple.of(streamId))
        // Use the stream
        stream.handler { row: Row ->
          latestVersion = row.getInteger("version")
          lastCausationId = row.getUUID("id")
          lastCorrelationId = row.getUUID("correlation_id")
          log.debug(
            "Found event version {}, causationId {}, correlationId {}",
            latestVersion,
            lastCausationId,
            lastCorrelationId,
          )
          state =
            eventHandler
              .invoke(state, eventSerDer.fromJson(row.getJsonObject("event_payload")))
          log.debug("State {}", state)
        }
        stream.exceptionHandler { error = it }
        stream.endHandler {
          stream.close()
          log.debug("End of stream")
          if (error != null) {
            promise.fail(error)
          } else {
            if (latestVersion == 0) {
              promise.complete(StreamSnapshot(streamId, state, latestVersion, null, null))
            } else {
              promise.complete(StreamSnapshot(streamId, state, latestVersion, lastCausationId!!, lastCorrelationId!!))
            }
          }
        }
        promise.future()
      }
  }

  companion object {
    const val NO_STREAM = -1
    private const val SQL_GET_EVENTS_BY_STREAM_ID =
      """
      SELECT id, event_type, event_payload, version, causation_id, correlation_id
        FROM events
       WHERE stream_id = $1
       ORDER BY sequence
    """
    private const val QUERY_MAX_STREAM_SIZE = 1000
  }
}

class StreamWriterImpl<S : Any, E : Any>(
  private val conn: SqlConnection,
  private val targetStream: TargetStream,
  private val streamId: Int,
  private val uuidFunction: () -> UUID,
  private val eventSerDer: JsonObjectSerDer<E>,
) : StreamWriter<S, E> {
  private val log = LoggerFactory.getLogger("${StreamWriterImpl::class.java.simpleName}-${targetStream.stateType}")

  override fun lockTargetStream(): Future<Int> {
    return conn
      .preparedQuery(SQL_LOCK)
      .execute(Tuple.of("streams_table".hashCode(), streamId.hashCode()))
      .compose { pgRow ->
        if (pgRow.first().getBoolean("locked")) {
          Future.succeededFuture(streamId)
        } else {
          Future.failedFuture(CrabzillaWriterException.StreamCantBeLockedException("Stream $streamId can't be locked"))
        }
      }
  }

  override fun appendEvents(
    streamSnapshot: StreamSnapshot<S>,
    events: List<E>,
  ): Future<List<EventRecord>> {
    log.debug("Will append events {}", events)
    var resultingVersion = streamSnapshot.version
    val eventIds = events.map { uuidFunction.invoke() }
    val causationIds = eventIds.toMutableList()
    val correlationIds = eventIds.toMutableList()
    val tuples: List<Tuple> =
      events.mapIndexed { index, event ->
        correlationIds[index] = streamSnapshot.correlationId ?: causationIds[0]
        val eventAsJsonObject = eventSerDer.toJson(event)
        val eventId = eventIds[index]
        val type = eventAsJsonObject.getString("type")
        if (index == 0) {
          causationIds[0] = streamSnapshot.causationId ?: eventIds[0]
        } else {
          causationIds[index] = eventIds[(index - 1)]
        }
        Tuple.of(
          type,
          causationIds[index],
          correlationIds[index],
          eventAsJsonObject,
          ++resultingVersion,
          eventId,
          streamId,
        )
      }
    val appendedEventList = mutableListOf<EventRecord>()
    return conn.preparedQuery(SQL_APPEND_EVENT)
      .executeBatch(tuples)
      .onSuccess { rowSet ->
        var rs: RowSet<Row>? = rowSet
        List(tuples.size) { index ->
          val sequence = rs!!.iterator().next().getLong("sequence")
          val correlationId = tuples[index].getUUID(CORRELATION_ID_INDEX)
          val currentVersion = tuples[index].getInteger(CURRENT_VERSION_INDEX)
          val eventId = tuples[index].getUUID(EVENT_ID_INDEX)
          val eventPayload = tuples[index].getJsonObject(EVENT_PAYLOAD_INDEX)
          val eventMetadata =
            EventMetadata(
              streamId = streamId,
              stateType = targetStream.stateType,
              stateId = targetStream.stateId,
              eventId = eventId,
              correlationId = correlationId,
              causationId = eventId,
              eventSequence = sequence,
              version = currentVersion,
              tuples[index].getString(0),
            )
          appendedEventList.add(EventRecord(eventMetadata, eventPayload))
          rs = rs!!.next()
        }
      }.map {
        appendedEventList
      }
  }

  companion object {
    const val SQL_GET_STREAM = """
         SELECT id
           FROM streams
          WHERE name = $1
    """
    const val SQL_INSERT_STREAM = """
      INSERT
        INTO streams (state_type, state_id, name)
      VALUES ($1, $2, $3) RETURNING id
    """
    private const val SQL_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
    private const val SQL_APPEND_EVENT =
      """ INSERT
            INTO events (event_type, causation_id, correlation_id, event_payload, version, id, stream_id)
          VALUES ($1, $2, $3, $4, $5, $6, $7) returning sequence"""
    private const val CORRELATION_ID_INDEX = 2
    private const val EVENT_PAYLOAD_INDEX = 3
    private const val CURRENT_VERSION_INDEX = 4
    private const val EVENT_ID_INDEX = 5
  }
}
