package io.github.crabzilla.stream

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.JsonObjectSerDer
import io.vertx.core.Future
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.time.ZoneOffset
import java.util.*

class StreamWriterImpl<S : Any, E : Any>(
  private val conn: SqlConnection,
  private val targetStream: TargetStream,
  private val streamId: Int,
  private val uuidFunction: () -> UUID,
  private val eventSerDer: JsonObjectSerDer<E>,
) : StreamWriter<S, E> {
  override fun lockTargetStream(lockingImplementation: StreamWriterLockEnum): Future<Int> {
    fun pgAdvisoryLock(): Future<Int> {
      return conn
        .preparedQuery(SQL_ADVISORY_LOCK)
        .execute(Tuple.of("streams_table".hashCode(), streamId.hashCode()))
        .compose { pgRow ->
          if (pgRow.first().getBoolean("locked")) {
            Future.succeededFuture(streamId)
          } else {
            Future.failedFuture(StreamCantBeLockedException("Stream $streamId can't be locked using $lockingImplementation"))
          }
        }
    }

    fun rowLevelLock(): Future<Int> {
      return conn.preparedQuery(SQL_ROW_LOCK)
        .execute(Tuple.of(streamId))
        .compose { pgRow ->
          if (pgRow.size() == 1) {
            Future.succeededFuture(streamId)
          } else {
            Future.failedFuture(StreamCantBeLockedException("Stream $streamId can't be locked using $lockingImplementation"))
          }
        }
    }
    return when (lockingImplementation) {
      StreamWriterLockEnum.PG_ADVISORY -> pgAdvisoryLock()
      StreamWriterLockEnum.PG_ROW_LEVEL -> rowLevelLock()
      StreamWriterLockEnum.BOTH_ONLY_THE_PARANOID_SURVIVE -> pgAdvisoryLock().compose { rowLevelLock() }
    }
  }

  override fun appendEvents(
    streamSnapshot: StreamSnapshot<S>,
    events: List<E>,
  ): Future<List<EventRecord>> {
    if (logger.isTraceEnabled) logger.trace("Will append events {}", events)
    var resultingVersion = streamSnapshot.version
    val eventIds = events.map { uuidFunction.invoke() }
    val causationIds = eventIds.toMutableList()
    val correlationIds = eventIds.toMutableList()
    val tuples: List<Tuple> =
      events.mapIndexed { index, event ->
        correlationIds[index] = streamSnapshot.correlationId ?: causationIds[0]
        val eventId = eventIds[index]
        val eventAsJsonObject = eventSerDer.toJson(event)
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
          val row = rs!!.iterator().next()
          val sequence = row.getLong("sequence")
          val instant = row.getLocalDateTime("created_at")
          val correlationId = tuples[index].getUUID(CORRELATION_ID_INDEX)
          val currentVersion = tuples[index].getInteger(CURRENT_VERSION_INDEX)
          val eventId = tuples[index].getUUID(EVENT_ID_INDEX)
          val eventPayload = tuples[index].getJsonObject(EVENT_PAYLOAD_INDEX)
          val eventMetadata =
            EventMetadata(
              streamId = streamId,
              stateType = targetStream.stateType(),
              stateId = targetStream.stateId(),
              eventId = eventId,
              correlationId = correlationId,
              causationId = eventId,
              eventSequence = sequence,
              version = currentVersion,
              eventType = tuples[index].getString(0),
              instant.toInstant(ZoneOffset.UTC),
            )
          appendedEventList.add(EventRecord(eventMetadata, eventPayload))
          rs = rs!!.next()
        }
      }.map {
        appendedEventList
      }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(StreamWriterImpl::class.java)
    const val SQL_GET_STREAM = """
         SELECT id
           FROM streams
          WHERE name = $1
    """
    private const val SQL_ADVISORY_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
    private const val SQL_ROW_LOCK =
      """ SELECT id FROM streams WHERE id = $1 FOR UPDATE NOWAIT
      """
    private const val SQL_APPEND_EVENT =
      """ INSERT
            INTO events (event_type, causation_id, correlation_id, event_payload, version, id, stream_id)
          VALUES ($1, $2, $3, $4, $5, $6, $7) returning sequence, created_at"""
    private const val CORRELATION_ID_INDEX = 2
    private const val EVENT_PAYLOAD_INDEX = 3
    private const val CURRENT_VERSION_INDEX = 4
    private const val EVENT_ID_INDEX = 5
  }
}
