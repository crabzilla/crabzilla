package io.github.crabzilla.pgclient.command.internal

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.core.json.JsonSerDer
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID

internal class OnDemandSnapshotRepo<S : State, E : Event>(
  private val eventHandler: EventHandler<S, E>,
  private val jsonSerDer: JsonSerDer,
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
            val event = jsonSerDer.eventFromJson(it.first) as E
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
