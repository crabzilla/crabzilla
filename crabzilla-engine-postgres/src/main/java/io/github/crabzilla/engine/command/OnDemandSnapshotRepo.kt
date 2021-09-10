package io.github.crabzilla.engine.command

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.core.serder.JsonSerDer
import io.github.crabzilla.stack.command.Snapshot
import io.vertx.core.Future
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID

class OnDemandSnapshotRepo<S : State, E : Event>(
  private val eventHandler: EventHandler<S, E>,
  private val jsonSerDer: JsonSerDer,
) :
  SnapshotRepository<S, E> {

  companion object {
    private val log = LoggerFactory.getLogger(OnDemandSnapshotRepo::class.java)
    private const val GET_EVENTS_BY_ID =
      """
      SELECT event_payload, version
      FROM events
      WHERE ar_id = $1
      ORDER BY sequence
    """
  }

  override fun get(pgConn: PgConnection, id: UUID): Future<Snapshot<S>?> {
    return pgConn
      .preparedQuery(GET_EVENTS_BY_ID)
      .execute(Tuple.of(id))
      .map { rowSet ->
        rowSet.iterator().asSequence().map { row: Row ->
          Pair(row.getValue("event_payload").toString(), row.getInteger("version"))
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
    conn: PgConnection,
    id: UUID,
    originalVersion: Int,
    resultingVersion: Int,
    newState: S,
  ): Future<Void> {
    return Future.succeededFuture()
  }
}
