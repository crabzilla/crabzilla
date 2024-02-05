package io.github.crabzilla.stream

import io.github.crabzilla.context.JsonObjectSerDer
import io.github.crabzilla.context.TargetStream
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.Row
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
  private val log =
    LoggerFactory.getLogger("${StreamRepositoryImpl::class.java.simpleName}-${targetStream.stateType()}")

  override fun getStreamId(): Future<Int> {
    val params = Tuple.of(targetStream.name)
    log.debug("Will get stream {}", targetStream.name)
    return conn.preparedQuery(StreamWriterImpl.SQL_GET_STREAM)
      .execute(params)
      .map {
        if (it.rowCount() == 0) {
          StreamRepository.NO_STREAM
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
        val stream: RowStream<Row> = pq.createStream(StreamRepository.QUERY_MAX_STREAM_SIZE, Tuple.of(streamId))
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
    private const val SQL_GET_EVENTS_BY_STREAM_ID =
      """
      SELECT id, event_type, event_payload, version, causation_id, correlation_id
        FROM events
       WHERE stream_id = $1
       ORDER BY sequence
    """
  }
}
