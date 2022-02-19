package io.github.crabzilla.pgclient.command.internal

import io.github.crabzilla.core.command.EventHandler
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

internal class OnDemandSnapshotRepo<S : Any, E : Any>(
  private val eventHandler: EventHandler<S, E>,
  private val json: Json,
  private val eventSerDer: PolymorphicSerializer<E>
) :
  SnapshotRepository<S, E> {

  companion object {
    private val log = LoggerFactory.getLogger(OnDemandSnapshotRepo::class.java)
    private const val GET_EVENTS_BY_ID =
      """
      SELECT event_type, event_payload, version
      FROM events
      WHERE state_id = $1
      ORDER BY sequence
    """
  }

  override fun get(conn: SqlConnection, id: UUID): Future<Snapshot<S>?> {
    return conn
      .preparedQuery(GET_EVENTS_BY_ID)
      .execute(Tuple.of(id))
      .map { rowSet ->
        rowSet
          .iterator()
          .asSequence()
          .map { row: Row ->
            val json = JsonObject(row.getValue("event_payload").toString())
            json.put("type", row.getString("event_type"))
            Pair(json.toString(), row.getInteger("version"))
          }.toList()
      }.compose { events ->
        if (events.isEmpty()) {
          Future.succeededFuture(null)
        } else {
          var state: S? = null
          events.forEach {
            val event = json.decodeFromString(eventSerDer, it.first)
            state = eventHandler.handleEvent(state, event)
          }
          Future.succeededFuture(Snapshot(state!!, events.last().second))
        }
      }
  }

  override fun upsert(
    conn: SqlConnection,
    id: UUID,
    originalVersion: Int,
    resultingVersion: Int,
    newState: S,
  ): Future<Void> {
    return Future.succeededFuture()
  }
}
