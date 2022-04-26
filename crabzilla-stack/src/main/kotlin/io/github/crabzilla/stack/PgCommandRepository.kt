package io.github.crabzilla.stack

import io.github.crabzilla.core.EventHandler
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import java.util.UUID
import kotlin.reflect.KClass

abstract class PgCommandRepository<S : Any, C : Any, E : Any> {

  companion object {
    const val GET_EVENTS_BY_ID =
      """
      SELECT event_type, event_payload, version
      FROM events
      WHERE state_id = $1
      ORDER BY sequence
    """
    const val SQL_APPEND_CMD =
      """ INSERT INTO commands (cmd_id, cmd_payload)
          VALUES ($1, $2)"""
    const val SQL_APPEND_EVENT =
      """ INSERT 
            INTO events (event_type, causation_id, correlation_id, state_type, state_id, event_payload, version, id)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8) returning sequence"""
    const val correlationIdIndex = 2
    const val eventPayloadIndex = 5
    const val currentVersionIndex = 6
    const val eventIdIndex = 7
  }

  abstract fun getSnapshot(conn: SqlConnection, id: UUID, eventClass: KClass<E>, eventHandler: EventHandler<S, E>,
      eventStreamSize: Int = 1000)
      : Future<Snapshot<S>?>

  abstract fun appendCommand(conn: SqlConnection, command: C, metadata: CommandMetadata)
      : Future<Void>

  abstract fun appendEvents(conn: SqlConnection, initialVersion: Int, events: List<E>,
                            metadata: CommandMetadata, stateTypeName: String)
      : Future<CommandSideEffect>
}